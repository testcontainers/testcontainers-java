package org.testcontainers.utility.ducttape;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

/**
 * A rate limiter that uses a simple 'run every n millis' strategy to achieve constant throughput.
 * This code comes from <a href="https://github.com/rnorth/duct-tape/">rnorth/duct-tape</a>
 */
class ConstantThroughputRateLimiter extends RateLimiter {

    private final long timeBetweenInvocations;

    ConstantThroughputRateLimiter(@NotNull Integer rate, @NotNull TimeUnit perTimeUnit) {
        this.timeBetweenInvocations = perTimeUnit.toMillis(1) / rate;
    }

    @Override
    protected long getWaitBeforeNextInvocation() {

        long timeToNextAllowed = (lastInvocation + timeBetweenInvocations) - System.currentTimeMillis();

        // Clamp wait time to 0<
        return Math.max(timeToNextAllowed, 0);
    }
}
