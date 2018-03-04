package org.testcontainers.containers;

import org.testcontainers.containers.wait.WaitStrategy;

import java.time.Duration;

public interface StartupTimeout<SELF> {

    /**
     * Set the duration of waiting time until container treated as started.
     * @see WaitStrategy#waitUntilReady(GenericContainer)
     *
     * @param startupTimeout timeout
     * @return this
     */
    SELF withStartupTimeout(Duration startupTimeout);
}
