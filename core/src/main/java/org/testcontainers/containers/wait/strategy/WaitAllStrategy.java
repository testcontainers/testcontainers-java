package org.testcontainers.containers.wait.strategy;

import org.rnorth.ducttape.timeouts.Timeouts;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class WaitAllStrategy implements WaitStrategy {

    private final List<WaitStrategy> strategies = new ArrayList<>();
    private Duration timeout = Duration.ofSeconds(30);

    @Override
    public void waitUntilReady(WaitStrategyTarget waitStrategyTarget) {
        Timeouts.doWithTimeout((int) timeout.toMillis(), TimeUnit.MILLISECONDS, () -> {
            for (WaitStrategy strategy : strategies) {
                strategy.waitUntilReady(waitStrategyTarget);
            }
        });
    }

    public WaitAllStrategy withStrategy(WaitStrategy strategy) {
        this.strategies.add(strategy);
        return this;
    }

    @Override
    public WaitStrategy withStartupTimeout(Duration startupTimeout) {
        this.timeout = startupTimeout;
        return this;
    }
}
