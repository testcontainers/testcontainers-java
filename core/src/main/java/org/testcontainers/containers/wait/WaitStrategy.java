package org.testcontainers.containers.wait;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;

import java.time.Duration;

/**
 * Approach to determine whether a container is ready.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 *
 * @deprecated Use {@link org.testcontainers.containers.wait.strategy.WaitStrategy}
 */
@Deprecated
public interface WaitStrategy extends org.testcontainers.containers.wait.strategy.WaitStrategy {
    /**
     * Wait until the container has started.
     *
     * @param container the container for which to wait
     */
    default void waitUntilReady(GenericContainer container) {
        this.waitUntilReady((WaitStrategyTarget)container);
    }

    /**
     * @param startupTimeout the duration for which to wait
     * @return this
     */
    WaitStrategy withStartupTimeout(Duration startupTimeout);

    /**
     * {@inheritDoc}
     */
    default void waitUntilReady(WaitStrategyTarget waitStrategyTarget) {
        //default method for backwards compatibility
    }
}
