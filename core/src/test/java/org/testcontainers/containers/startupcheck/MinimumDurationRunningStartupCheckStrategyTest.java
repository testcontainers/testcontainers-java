package org.testcontainers.containers.startupcheck;

import org.junit.jupiter.api.Test;
import org.testcontainers.TestImages;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MinimumDurationRunningStartupCheckStrategyTest {

    @Test
    void shouldPassStartupCheckIfContainerExceedsMinimumRunningDuration() {
        try (
            GenericContainer container = new GenericContainer<>(TestImages.TINY_IMAGE)
                .withCommand("sleep", "1")
                .withMinimumRunningDuration(Duration.ofMillis(100))
        ) {
            assertThatNoException().isThrownBy(container::start);
        }
    }

    @Test
    void shouldFailStartupCheckIfContainerExitsPrematurely() {
        try (
            GenericContainer container = new GenericContainer<>(TestImages.TINY_IMAGE)
                .withCommand("/bin/false")
                .withMinimumRunningDuration(Duration.ofMillis(500))
        ) {
            assertThatThrownBy(container::start)
                .isInstanceOf(ContainerLaunchException.class)
                .hasStackTraceContaining("Container startup failed")
                .hasStackTraceContaining("Container did not start correctly");
        }
    }
}
