package org.testcontainers.containers.wait;

import org.rnorth.ducttape.timeouts.Timeouts;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Wait strategy that waits for a number of other strategies to pass in series.
 *
 * @deprecated Use {@link org.testcontainers.containers.wait.strategy.WaitAllStrategy}
 */
@Deprecated
public class WaitAllStrategy implements WaitStrategy {

    private final List<WaitStrategy> strategies = new ArrayList<>();
    private Duration timeout = Duration.ofSeconds(30);

    @Override
    public void waitUntilReady(GenericContainer container) {
        Timeouts.doWithTimeout((int) timeout.toMillis(), TimeUnit.MILLISECONDS, () -> {
            for (WaitStrategy strategy : strategies) {
                strategy.waitUntilReady(container);
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
