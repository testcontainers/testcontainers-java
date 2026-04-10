package org.testcontainers.junit.jupiter;

import org.testcontainers.containers.GenericContainer;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Base class for tests that verify {@link SharedContainers} shares containers across test classes.
 *
 * <p>The {@code observedContainerId} captures the first container ID seen by any subclass.
 * Each subclass then verifies that its container has the same ID, proving the container was
 * started only once across all test classes in the JVM session.</p>
 */
@SharedContainers
abstract class SharedContainersBaseTest {

    @Container
    static final GenericContainer<?> SHARED = new GenericContainer<>(JUnitJupiterTestImages.HTTPD_IMAGE)
        .withExposedPorts(80);

    /**
     * Tracks the container ID observed by the first test class to run.
     * Subsequent test classes compare their container ID against this value.
     */
    static final AtomicReference<String> observedContainerId = new AtomicReference<>();
}
