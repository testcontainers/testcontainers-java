package org.testcontainers.containers.wait;

import org.testcontainers.containers.GenericContainer;

/**
 * Waits until containers logs expected content.
 *
 * @deprecated Use {@link org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy}
 */
@Deprecated
public class LogMessageWaitStrategy extends GenericContainer.AbstractWaitStrategy {

    private org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy delegateWaitStrategy = new org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy();

    @Override
    protected void waitUntilReady() {
        delegateWaitStrategy.waitUntilReady(this.waitStrategyTarget);
    }

    public LogMessageWaitStrategy withRegEx(String regEx) {
        delegateWaitStrategy.withRegEx(regEx);
        return this;
    }

    public LogMessageWaitStrategy withTimes(int times) {
        delegateWaitStrategy.withTimes(times);
        return this;
    }
}
