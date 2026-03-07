package org.testcontainers.junit;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class DockerComposeErrorHandlingTest {

    @Test
    void simpleTest() {
        assertThat(
            catchThrowable(() -> {
                DockerComposeContainer environment = new DockerComposeContainer(
                    DockerImageName.parse("docker/compose:1.29.2"),
                    new File("src/test/resources/invalid-compose.yml")
                )
                    .withExposedService("something", 123);
            })
        )
            .as("starting with an invalid docker-compose file throws an exception")
            .isInstanceOf(IllegalArgumentException.class);
    }
}
