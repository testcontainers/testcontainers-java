package org.testcontainers.junit;

import org.junit.Test;
import org.testcontainers.containers.ComposeContainer;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class ComposeErrorHandlingTest {

    @Test
    public void simpleTest() {
        assertThat(
            catchThrowable(() -> {
                ComposeContainer environment = new ComposeContainer(new File("src/test/resources/invalid-compose.yml"))
                    .withExposedService("something", 123);
            })
        )
            .as("starting with an invalid docker-compose file throws an exception")
            .isInstanceOf(IllegalArgumentException.class);
    }
}
