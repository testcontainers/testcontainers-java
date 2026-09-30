package org.testcontainers.containers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

class ParsedDockerComposeFileValidationTest {

    @TempDir
    public Path temporaryFolder;

    @Test
    void shouldValidate() {
        File file = new File("src/test/resources/docker-compose-container-name-v1.yml");
        assertThatThrownBy(() -> {
                new ParsedDockerComposeFile(file);
            })
            .hasMessageContaining(file.getAbsolutePath())
            .hasMessageContaining("'container_name' property set for service 'redis'");
    }

    @Test
    void shouldRejectContainerNameV1() {
        assertThatThrownBy(() -> {
                new ParsedDockerComposeFile(ImmutableMap.of("redis", ImmutableMap.of("container_name", "redis")));
            })
            .hasMessageContaining("'container_name' property set for service 'redis'");
    }

    @Test
    void shouldRejectContainerNameV2() {
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
    void shouldIgnoreUnknownStructure() {
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
    void shouldRejectDeserializationOfArbitraryClasses() {
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
    void shouldObtainImageNamesV1() {
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
    void shouldObtainImageNamesV2() {
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
    void shouldObtainImageNamesV2WithNoVersionTag() {
        File file = new File("src/test/resources/docker-compose-imagename-parsing-v2-no-version.yml");
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
    void shouldObtainImageFromDockerfileBuild() {
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
    void shouldObtainImageFromDockerfileBuildWithContext() {
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
    void shouldObtainImageNamesFromOverrideTag() throws Exception {
        File file = temporaryFolder.resolve("override-docker-compose.yml").toFile();
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println("services:");
            writer.println("  db: !override");
            writer.println("    image: postgres:16");
            writer.println("  redis:");
            writer.println("    image: !override redis:7");
        }
        ParsedDockerComposeFile parsedFile = new ParsedDockerComposeFile(file);
        assertThat(parsedFile.getServiceNameToImageNames())
            .as("values tagged with !override are kept")
            .containsOnly(entry("db", Sets.newHashSet("postgres:16")), entry("redis", Sets.newHashSet("redis:7")));
    }

    @Test
    void shouldKeepQuotedValuesTaggedWithOverrideAsStrings() throws Exception {
        File file = temporaryFolder.resolve("override-quoted-docker-compose.yml").toFile();
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println("services:");
            writer.println("  db:");
            writer.println("    image: !override \"1.0\"");
        }
        ParsedDockerComposeFile parsedFile = new ParsedDockerComposeFile(file);
        assertThat(parsedFile.getServiceNameToImageNames())
            .as("quoted values tagged with !override are not implicitly typed")
            .containsOnly(entry("db", Sets.newHashSet("1.0")));
    }

    @Test
    void shouldIgnoreImageNamesRemovedWithResetTag() throws Exception {
        File file = temporaryFolder.resolve("reset-docker-compose.yml").toFile();
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println("services:");
            writer.println("  db:");
            writer.println("    image: !reset null");
            writer.println("  redis:");
            writer.println("    image: redis:7");
        }
        ParsedDockerComposeFile parsedFile = new ParsedDockerComposeFile(file);
        assertThat(parsedFile.getServiceNameToImageNames())
            .as("values tagged with !reset are removed")
            .containsOnly(entry("redis", Sets.newHashSet("redis:7")));
    }

    @Test
    void shouldContinueAfterServiceWithUnknownStructure() {
        ParsedDockerComposeFile parsedFile = new ParsedDockerComposeFile(
            ImmutableMap.of(
                "services",
                ImmutableMap.of("unknown", "not a map", "redis", ImmutableMap.of("image", "redis:7"))
            )
        );
        assertThat(parsedFile.getServiceNameToImageNames())
            .as("services after one with an unknown structure are still parsed")
            .containsOnly(entry("redis", Sets.newHashSet("redis:7")));
    }

    @Test
    void shouldIgnoreTopLevelElementsWithoutServicesElement() throws Exception {
        File file = temporaryFolder.resolve("no-services-docker-compose.yml").toFile();
        try (PrintWriter writer = new PrintWriter(file)) {
            writer.println("version: \"3.8\"");
            writer.println("x-common:");
            writer.println("  image: busybox:1.36");
            writer.println("networks:");
            writer.println("  backend: {}");
        }
        ParsedDockerComposeFile parsedFile = new ParsedDockerComposeFile(file);
        assertThat(parsedFile.getServiceNameToImageNames())
            .as("extensions and top-level elements are not services")
            .isEmpty();
    }

    @Test
    void shouldSupportALotOfAliases() throws Exception {
        File file = temporaryFolder.resolve("tmp-docker-compose.yml").toFile();
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
