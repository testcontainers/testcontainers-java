package org.testcontainers.containers;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Tests for in-memory log capture in {@link GenericContainer}.
 * <p>
 * These tests verify that container logs can be captured even when container startup fails or the
 * container exits immediately after producing output.
 */
class GenericContainerLogCaptureTest {

    private static final DockerImageName TEST_IMAGE =
        DockerImageName.parse("busybox:1.36");

    private static final String CONTAINER_LOG_MESSAGE =
        "container startup output";

    /**
     * A log pattern that is intentionally never produced by the container. Used to force startup
     * failure via {@link Wait#forLogMessage(String, int)}.
     */
    private static final String NON_MATCHING_LOG_PATTERN =
        "this-log-message-will-never-appear";

    private static final Duration STARTUP_TIMEOUT =
        Duration.ofSeconds(2);

    @Test
    void shouldNotCaptureLogsByDefaultWhenStartupFails() {
        try (GenericContainer<?> container = createFailingContainer()) {

            Assertions.assertThrows(
                ContainerLaunchException.class,
                container::start,
                "Container startup should fail due to unmet wait condition"
            );

            Assertions.assertTrue(
                container.getCapturedLogs().isEmpty(),
                "Captured logs should be empty when log capture is not enabled"
            );
        }
    }

    @Test
    void shouldCaptureLogsWhenEnabledEvenIfStartupFails() {
        try (GenericContainer<?> container =
            createFailingContainer().withLogCapture()) {

            Assertions.assertThrows(
                ContainerLaunchException.class,
                container::start,
                "Container startup should fail due to unmet wait condition"
            );

            String capturedLogs = container.getCapturedLogs();

            Assertions.assertFalse(
                capturedLogs.isEmpty(),
                "Captured logs should not be empty when log capture is enabled"
            );

            Assertions.assertTrue(
                capturedLogs.contains(CONTAINER_LOG_MESSAGE),
                "Captured logs should contain the container output"
            );
        }
    }

    /**
     * Creates a container configuration that:
     * <ul>
     *   <li>Produces a single line of output</li>
     *   <li>Exits immediately</li>
     *   <li>Fails startup due to an unmet log-based wait condition</li>
     * </ul>
     *
     * @return a configured {@link GenericContainer} instance
     */
    private static GenericContainer<?> createFailingContainer() {
        return new GenericContainer<>(TEST_IMAGE)
            .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
            .waitingFor(
                Wait.forLogMessage(NON_MATCHING_LOG_PATTERN, 1)
                    .withStartupTimeout(STARTUP_TIMEOUT)
            )
            .withCommand("sh", "-c", "echo \"" + CONTAINER_LOG_MESSAGE + "\"");
    }
}

