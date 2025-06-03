package org.testcontainers.containers.startupcheck;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.TestImages;
import org.testcontainers.containers.GenericContainer;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class IsRunningStartupCheckStrategyTest {

    @Test
    public void testCommandQuickExitSuccess() {
        try (GenericContainer container = new GenericContainer<>(TestImages.TINY_IMAGE).withCommand("/bin/true")) {
            container.start(); // should start with no Exception
        }
    }

    @Test
    @Disabled("This test can fail to throw an AssertionError if the container doesn't fail quickly enough")
    public void testCommandQuickExitFailure() {
        try (GenericContainer container = new GenericContainer<>(TestImages.TINY_IMAGE).withCommand("/bin/false")) {
            assertThatThrownBy(container::start)
                .hasStackTraceContaining("Container startup failed")
                .hasStackTraceContaining("Container did not start correctly");
        }
    }

    @Test
    public void testCommandStaysRunning() {
        try (
            GenericContainer container = new GenericContainer<>(TestImages.TINY_IMAGE).withCommand("/bin/sleep", "60")
        ) {
            container.start(); // should start with no Exception
        }
    }
}
