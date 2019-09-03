package org.testcontainers.containers;

import com.google.common.collect.ImmutableMap;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import java.io.File;

import static java.util.Collections.emptyList;

public class DockerComposeFileValidationTest {

    @Test
    public void shouldValidate() {
        File file = new File("src/test/resources/docker-compose-container-name-v1.yml");
        try (DockerComposeContainer container = new DockerComposeContainer<>(file)) {
            Assertions.assertThatThrownBy(container::start)
                .hasMessageContaining(file.getAbsolutePath())
                .hasMessageContaining("'container_name' property set for service 'redis'");
        }
    }

    @Test
    public void shouldRejectContainerNameV1() {
        Assertions
            .assertThatThrownBy(() -> {
                DockerComposeContainer.validate(ImmutableMap.of(
                    "redis", ImmutableMap.of("container_name", "redis")
                ), "");
            })
            .hasMessageContaining("'container_name' property set for service 'redis'");
    }

    @Test
    public void shouldRejectContainerNameV2() {
        Assertions
            .assertThatThrownBy(() -> {
                DockerComposeContainer.validate(ImmutableMap.of(
                    "version", "2",
                    "services", ImmutableMap.of(
                        "redis", ImmutableMap.of("container_name", "redis")
                    )
                ), "");
            })
            .hasMessageContaining("'container_name' property set for service 'redis'");
    }

    @Test
    public void shouldIgnoreUnknownStructure() {
        // Everything is a list
        DockerComposeContainer.validate(emptyList(), "");

        // services is not a map but List
        DockerComposeContainer.validate(ImmutableMap.of(
            "version", "2",
            "services", emptyList()
        ), "");

        // services is not a collection
        DockerComposeContainer.validate(ImmutableMap.of(
            "version", "2",
            "services", true
        ), "");

        // no services while version is defined
        DockerComposeContainer.validate(ImmutableMap.of(
            "version", "9000"
        ), "");
    }
}
