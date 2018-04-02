package org.testcontainers.containers.wait.strategy;

import java.time.Duration;

public interface WaitStrategy {

    void waitUntilReady(WaitStrategyTarget waitStrategyTarget);

    WaitStrategy withStartupTimeout(Duration startupTimeout);
}
