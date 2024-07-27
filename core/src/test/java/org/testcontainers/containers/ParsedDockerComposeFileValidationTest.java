package org.testcontainers.containers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import lombok.SneakyThrows;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.PrintWriter;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

public class ParsedDockerComposeFileValidationTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void shouldValidate() {
        File file = new File("src/test/resources/docker-compose-container-name-v1.yml");
        assertThatThrownBy(() -> {
                new ParsedDockerComposeFile(file);
            })
            .hasMessageContaining(file.getAbsolutePath())
            .hasMessageContaining("'container_name' property set for service 'redis'");
    }

    @Test
    public void shouldRejectContainerNameV1() {
        assertThatThrownBy(() -> {
                new ParsedDockerComposeFile(ImmutableMap.of("redis", ImmutableMap.of("container_name", "redis")));
            })
            .hasMessageContaining("'container_name' property set for service 'redis'");
    }

    @Test
    public void shouldRejectContainerNameV2() {
        assertThatThrownBy(() -> {
                new ParsedDockerComposeFile(
                    ImmutableMap.of(
                        "version",
                        "2",
                        "services",
                        ImmutableMap.of("redis", ImmutableMap.of("container_name", "redis"))
                    )
                );
            })
            .hasMessageContaining("'container_name' property set for service 'redis'");
    }

    @Test
    public void shouldIgnoreUnknownStructure() {
        // Everything is a list
        new ParsedDockerComposeFile(Collections.emptyMap());

        // services is not a map but List
        new ParsedDockerComposeFile(ImmutableMap.of("version", "2", "services", Collections.emptyList()));

        // services is not a collection
        new ParsedDockerComposeFile(ImmutableMap.of("version", "2", "services", true));

        // no services while version is defined
        new ParsedDockerComposeFile(ImmutableMap.of("version", "9000"));
    }

    @Test
    @SneakyThrows
    public void shouldRejectDeserializationOfArbitraryClasses() {
        // Reject deserialization gadget chain attacks: https://nvd.nist.gov/vuln/detail/CVE-2022-1471
        // https://raw.githubusercontent.com/mbechler/marshalsec/master/marshalsec.pdf

        File file = new File("src/test/resources/docker-compose-deserialization.yml");

        // ParsedDockerComposeFile should reject deserialization of ParsedDockerComposeFileBean
        assertThatThrownBy(() -> {
                new ParsedDockerComposeFile(file);
            })
            .hasMessageContaining(file.getAbsolutePath())
            .hasMessageContaining("Unable to parse YAML file");
    }

    @Test
    public void shouldObtainImageNamesV1() {
        File file = new File("src/test/resources/docker-compose-imagename-parsing-v1.yml");
        ParsedDockerComposeFile parsedFile = new ParsedDockerComposeFile(file);
        assertThat(parsedFile.getServiceNameToImageNames())
            .as("all defined images are found")
            .contains(
                entry("mysql", Sets.newHashSet("mysql")),
                entry("redis", Sets.newHashSet("redis")),
                entry("custom", Sets.newHashSet("postgres"))
            ); // redis, mysql from compose file, postgres from Dockerfile build
    }

    @Test
    public void shouldObtainImageNamesV2() {
        File file = new File("src/test/resources/docker-compose-imagename-parsing-v2.yml");
        ParsedDockerComposeFile parsedFile = new ParsedDockerComposeFile(file);
        assertThat(parsedFile.getServiceNameToImageNames())
            .as("all defined images are found")
            .contains(
                entry("mysql", Sets.newHashSet("mysql")),
                entry("redis", Sets.newHashSet("redis")),
                entry("custom", Sets.newHashSet("postgres"))
            );
    }

    @Test
    public void shouldObtainImageFromDockerfileBuild() {
        File file = new File("src/test/resources/docker-compose-imagename-parsing-dockerfile.yml");
        ParsedDockerComposeFile parsedFile = new ParsedDockerComposeFile(file);
        assertThat(parsedFile.getServiceNameToImageNames())
            .as("all defined images are found")
            .contains(
                entry("mysql", Sets.newHashSet("mysql")),
                entry("redis", Sets.newHashSet("redis")),
                entry("custom", Sets.newHashSet("alpine:3.17"))
            ); // r/ redis, mysql from compose file, alpine:3.17 from Dockerfile build
    }

    @Test
    public void shouldObtainImageFromDockerfileBuildWithContext() {
        File file = new File("src/test/resources/docker-compose-imagename-parsing-dockerfile-with-context.yml");
        ParsedDockerComposeFile parsedFile = new ParsedDockerComposeFile(file);
        assertThat(parsedFile.getServiceNameToImageNames())
            .as("all defined images are found")
            .contains(
                entry("mysql", Sets.newHashSet("mysql")),
                entry("redis", Sets.newHashSet("redis")),
                entry("custom", Sets.newHashSet("alpine:3.17"))
            ); // redis, mysql from compose file, alpine:3.17 from Dockerfile build
    }

    @Test
    public void shouldSupportALotOfAliases() throws Exception {
        File file = temporaryFolder.newFile();
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println("x-entry: &entry");
            writer.println("  key: value");
            writer.println();
            writer.println("services:");
            for (int i = 0; i < 1_000; i++) {
                writer.println("  service" + i + ":");
                writer.println("    image: busybox");
                writer.println("    environment:");
                writer.println("      <<: *entry");
            }
        }
        assertThatNoException().isThrownBy(() -> new ParsedDockerComposeFile(file));
    }
}
