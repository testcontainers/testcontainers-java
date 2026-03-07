package org.testcontainers.junit;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class ComposeErrorHandlingTest {

    @Test
    void simpleTest() {
        assertThat(
            catchThrowable(() -> {
                ComposeContainer environment = new ComposeContainer(
                    DockerImageName.parse("docker:25.0.5"),
                    new File("src/test/resources/invalid-compose.yml")
                )
                    .withExposedService("something", 123);
            })
        )
            .as("starting with an invalid docker-compose file throws an exception")
            .isInstanceOf(IllegalArgumentException.class);
    }
}
