package org.testcontainers.containers;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.File;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.entry;

public class ParsedDockerComposeFileValidationTest {

    @Test
    public void shouldValidate() {
        File file = new File("src/test/resources/docker-compose-container-name-v1.yml");
        Assertions
            .assertThatThrownBy(() -> {
                new ParsedDockerComposeFile(file);
            })
            .hasMessageContaining(file.getAbsolutePath())
            .hasMessageContaining("'container_name' property set for service 'redis'");
    }

    @Test
    public void shouldRejectContainerNameV1() {
        Assertions
            .assertThatThrownBy(() -> {
                new ParsedDockerComposeFile(ImmutableMap.of(
                    "redis", ImmutableMap.of("container_name", "redis")
                ));
            })
            .hasMessageContaining("'container_name' property set for service 'redis'");
    }

    @Test
    public void shouldRejectContainerNameV2() {
        Assertions
            .assertThatThrownBy(() -> {
                new ParsedDockerComposeFile(ImmutableMap.of(
                    "version", "2",
                    "services", ImmutableMap.of(
                        "redis", ImmutableMap.of("container_name", "redis")
                    )
                ));
            })
            .hasMessageContaining("'container_name' property set for service 'redis'");
    }

    @Test
    public void shouldIgnoreUnknownStructure() {
        // Everything is a list
        new ParsedDockerComposeFile(emptyMap());

        // services is not a map but List
        new ParsedDockerComposeFile(ImmutableMap.of(
            "version", "2",
            "services", emptyList()
        ));

        // services is not a collection
        new ParsedDockerComposeFile(ImmutableMap.of(
            "version", "2",
            "services", true
        ));

        // no services while version is defined
        new ParsedDockerComposeFile(ImmutableMap.of(
            "version", "9000"
        ));
    }

    @Test
    public void shouldObtainImageNamesV1() {
        File file = new File("src/test/resources/docker-compose-imagename-parsing-v1.yml");
        ParsedDockerComposeFile parsedFile = new ParsedDockerComposeFile(file);
        Assertions.assertThat(parsedFile.getServiceNameToImageNames())
            .contains(
                entry("mysql", Sets.newHashSet("mysql")),
                entry("redis", Sets.newHashSet("redis")),
                entry("custom", Sets.newHashSet("postgres")))
            .as("all defined images are found"); // redis, mysql from compose file, postgres from Dockerfile build
    }

    @Test
    public void shouldObtainImageNamesV2() {
        File file = new File("src/test/resources/docker-compose-imagename-parsing-v2.yml");
        ParsedDockerComposeFile parsedFile = new ParsedDockerComposeFile(file);
        Assertions.assertThat(parsedFile.getServiceNameToImageNames())
            .contains(
                entry("mysql", Sets.newHashSet("mysql")),
                entry("redis", Sets.newHashSet("redis")),
                entry("custom", Sets.newHashSet("postgres")))
            .as("all defined images are found");
    }

    @Test
    public void shouldObtainImageFromDockerfileBuild() {
        File file = new File("src/test/resources/docker-compose-imagename-parsing-dockerfile.yml");
        ParsedDockerComposeFile parsedFile = new ParsedDockerComposeFile(file);
        Assertions.assertThat(parsedFile.getServiceNameToImageNames())
            .contains(
                entry("mysql", Sets.newHashSet("mysql")),
                entry("redis", Sets.newHashSet("redis")),
                entry("custom", Sets.newHashSet("alpine:3.14")))
            .as("all defined images are found"); // r/ redis, mysql from compose file, alpine:3.14 from Dockerfile build
    }

    @Test
    public void shouldObtainImageFromDockerfileBuildWithContext() {
        File file = new File("src/test/resources/docker-compose-imagename-parsing-dockerfile-with-context.yml");
        ParsedDockerComposeFile parsedFile = new ParsedDockerComposeFile(file);
        Assertions.assertThat(parsedFile.getServiceNameToImageNames())
            .contains(
                entry("mysql", Sets.newHashSet("mysql")),
                entry("redis", Sets.newHashSet("redis")),
                entry("custom", Sets.newHashSet("alpine:3.14")))
            .as("all defined images are found"); // redis, mysql from compose file, alpine:3.14 from Dockerfile build
    }
}
