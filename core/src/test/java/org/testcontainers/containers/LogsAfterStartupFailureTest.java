package org.testcontainers.containers;

import org.junit.jupiter.api.Test;
import org.testcontainers.TestImages;
import org.testcontainers.containers.wait.strategy.Wait;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogsAfterStartupFailureTest {

    @Test
    void shouldKeepLogsAfterStartupFailure() {
        try (
            GenericContainer<?> container = new GenericContainer<>(TestImages.TINY_IMAGE)
                .withCommand("sh", "-c", "echo 'something went wrong'; exit 1")
                .waitingFor(
                    Wait.forLogMessage(".*this will never appear.*", 1).withStartupTimeout(Duration.ofSeconds(10))
                )
        ) {
            assertThatThrownBy(container::start).isInstanceOf(ContainerLaunchException.class);

            assertThat(container.getLogs())
                .as("log output of the failed container is still available after startup failed")
                .contains("something went wrong");
        }
    }
}
