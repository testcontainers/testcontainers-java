package org.testcontainers.containers.wait.strategy;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.rnorth.ducttape.timeouts.Timeouts;

public class WaitAllStrategy implements WaitStrategy {

    public enum Mode {

        /**
         * This is the default mode: The timeout of the {@link WaitAllStrategy strategy} is applied to each individual
         * strategy, so that the container waits maximum for
         * {@link org.testcontainers.containers.wait.strategy.WaitAllStrategy#timeout}.
         */
        WITH_OUTER_TIMEOUT,

        /**
         * Using this mode triggers the following behaviour: The outer timeout is disabled and the outer enclosing
         * strategy waits for all inner strategies according to their timeout. Once set, it disables
         * {@link org.testcontainers.containers.wait.strategy.WaitAllStrategy#withStartupTimeout(java.time.Duration)} method,
         * as it would overwrite inner timeouts.
         */
        WITH_INDIVIDUAL_TIMEOUTS_ONLY,

        /**
         * This is the original mode of this strategy: The inner strategies wait with their preconfigured timeout
         * individually and the wait all strategy kills them, if the outer limit is reached.
         */
        WITH_MAXIMUM_OUTER_TIMEOUT
    }

    private final Mode mode;
    private final List<WaitStrategy> strategies = new ArrayList<>();
    private Duration timeout = Duration.ofSeconds(30);

    public WaitAllStrategy() {
        this(Mode.WITH_OUTER_TIMEOUT);
    }

    public WaitAllStrategy(Mode mode) {
        this.mode = mode;
    }

    @Override
    public void waitUntilReady(WaitStrategyTarget waitStrategyTarget) {
        if (mode == Mode.WITH_INDIVIDUAL_TIMEOUTS_ONLY) {
            waitUntilNestedStrategiesAreReady(waitStrategyTarget);
        } else {
            Timeouts.doWithTimeout((int) timeout.toMillis(), TimeUnit.MILLISECONDS, () -> {
                waitUntilNestedStrategiesAreReady(waitStrategyTarget);
            });
        }
    }

    private void waitUntilNestedStrategiesAreReady(WaitStrategyTarget waitStrategyTarget) {
        for (WaitStrategy strategy : strategies) {
            strategy.waitUntilReady(waitStrategyTarget);
        }
    }

    public WaitAllStrategy withStrategy(WaitStrategy strategy) {

        if (mode == Mode.WITH_OUTER_TIMEOUT) {
            applyStartupTimeout(strategy);
        }

        this.strategies.add(strategy);
        return this;
    }

    @Override
    public WaitAllStrategy withStartupTimeout(Duration startupTimeout) {

        if (mode == Mode.WITH_INDIVIDUAL_TIMEOUTS_ONLY) {
            throw new IllegalStateException(String.format(
                "Changing startup timeout is not supported with mode %s", Mode.WITH_INDIVIDUAL_TIMEOUTS_ONLY));
        }

        this.timeout = startupTimeout;
        strategies.forEach(this::applyStartupTimeout);
        return this;
    }

    private void applyStartupTimeout(WaitStrategy childStrategy) {
        childStrategy.withStartupTimeout(this.timeout);
    }
}
