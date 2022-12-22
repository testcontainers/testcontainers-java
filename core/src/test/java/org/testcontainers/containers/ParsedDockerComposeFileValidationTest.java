package org.testcontainers.containers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.junit.Test;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.util.Collections;
import lombok.SneakyThrows;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;

public class ParsedDockerComposeFileValidationTest {

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

        // SnakeYaml can deserialize ParsedDockerComposeFileBean when using permissive options
        try (Reader reader = new FileReader(file)) {
            new Yaml(new Constructor(new LoaderOptions())).load(reader);
        }

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
                entry("custom", Sets.newHashSet("alpine:3.16"))
            ); // r/ redis, mysql from compose file, alpine:3.16 from Dockerfile build
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
                entry("custom", Sets.newHashSet("alpine:3.16"))
            ); // redis, mysql from compose file, alpine:3.16 from Dockerfile build
    }
}
