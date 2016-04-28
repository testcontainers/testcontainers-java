package org.testcontainers.containers.wait;

import org.testcontainers.containers.GenericContainer;

import java.time.Duration;

/**
 * Approach to determine whether a container is ready.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public interface WaitStrategy {
    /**
     * Wait until the container has started.
     *
     * @param container the container for which to wait
     */
    void waitUntilReady(GenericContainer container);

    /**
     * @param startupTimeout the duration for which to wait
     * @return this
     */
    WaitStrategy withStartupTimeout(Duration startupTimeout);
}
