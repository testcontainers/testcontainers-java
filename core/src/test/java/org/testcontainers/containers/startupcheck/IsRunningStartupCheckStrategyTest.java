package org.testcontainers.containers.startupcheck;

import org.junit.jupiter.api.Test;
import org.testcontainers.TestImages;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IsRunningStartupCheckStrategyTest {

    @Test
    void testCommandQuickExitSuccess() {
        try (GenericContainer container = new GenericContainer<>(TestImages.TINY_IMAGE).withCommand("/bin/true")) {
            container.start(); // should start with no Exception
        }
    }

    @Test
    void testCommandQuickExitFailure() {
        try (GenericContainer container = new GenericContainer<>(TestImages.TINY_IMAGE).withCommand("/bin/false")) {
            assertThatThrownBy(container::start)
                .hasStackTraceContaining("Container startup failed")
                .hasStackTraceContaining("Container did not start correctly");
        }
    }

    @Test
    void testCommandStaysRunning() {
        try (
            GenericContainer container = new GenericContainer<>(TestImages.TINY_IMAGE).withCommand("/bin/sleep", "60")
        ) {
            container.start(); // should start with no Exception
        }
    }

    @Test
    void testQuickExitWithDifferentExitCode() {
        try (
            GenericContainer container = new GenericContainer<>(TestImages.TINY_IMAGE)
                .withCommand("/bin/sh", "-c", "exit 42")
        ) {
            assertThatThrownBy(container::start)
                .hasStackTraceContaining("Container startup failed")
                .hasStackTraceContaining("Container did not start correctly");
        }
    }
}
