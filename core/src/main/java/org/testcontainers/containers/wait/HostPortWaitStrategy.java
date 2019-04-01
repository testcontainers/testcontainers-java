package org.testcontainers.containers.wait;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;

/**
 * Waits until a socket connection can be established on a port exposed or mapped by the container.
 *
 * @author richardnorth
 *
 * @deprecated Use {@link org.testcontainers.containers.wait.strategy.HostPortWaitStrategy}
 */
@Deprecated
@Slf4j
public class HostPortWaitStrategy extends GenericContainer.AbstractWaitStrategy {

    private org.testcontainers.containers.wait.strategy.HostPortWaitStrategy delegateStrategy = new org.testcontainers.containers.wait.strategy.HostPortWaitStrategy();

    @Override
    protected void waitUntilReady() {
        delegateStrategy.waitUntilReady(this.waitStrategyTarget);
    }

    @Override
    public WaitStrategy withStartupTimeout(Duration startupTimeout) {
        delegateStrategy.withStartupTimeout(startupTimeout);
        return super.withStartupTimeout(startupTimeout);
    }
}
