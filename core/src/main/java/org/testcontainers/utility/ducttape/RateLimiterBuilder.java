package org.testcontainers.utility.ducttape;

import java.util.concurrent.TimeUnit;

import static org.testcontainers.utility.ducttape.Preconditions.check;

/**
 * Builder for rate limiters.
 * This code comes from <a href="https://github.com/rnorth/duct-tape/">rnorth/duct-tape</a>
 */
public class RateLimiterBuilder {

    private Integer invocations;
    private TimeUnit perTimeUnit;
    private RateLimiterStrategy strategy;

    private RateLimiterBuilder() { }

    /**
     * Obtain a new builder instance.
     * @return a new builder
     */
    public static RateLimiterBuilder newBuilder() {
        return new RateLimiterBuilder();
    }

    /**
     * Set the maximum rate that the limiter should allow, expressed as the number of invocations
     * allowed in a given time period.
     * @param invocations   number of invocations
     * @param perTimeUnit   the time period in which this number of invocations are allowed
     * @return the builder
     */
    public RateLimiterBuilder withRate(final int invocations, final TimeUnit perTimeUnit) {
        this.invocations = invocations;
        this.perTimeUnit = perTimeUnit;
        return this;
    }

    /**
     * Configure the rate limiter to use a constant throughput strategy for rate limiting.
     * @return the builder
     */
    public RateLimiterBuilder withConstantThroughput() {
        this.strategy = RateLimiterStrategy.CONSTANT_THROUGHPUT;
        return this;
    }

    /**
     * Build and obtain a configured rate limiter. A rate and rate limiting strategy must have been selected.
     * @return the configured rate limiter instance
     */
    public RateLimiter build() {
        check("A rate must be set", invocations != null);
        check("A rate must be set", perTimeUnit != null);
        check("A rate limit strategy must be set", strategy != null);

        if (strategy == RateLimiterStrategy.CONSTANT_THROUGHPUT) {
            return new ConstantThroughputRateLimiter(invocations, perTimeUnit);
        } else {
            throw new IllegalStateException();
        }
    }

    private enum RateLimiterStrategy {
        CONSTANT_THROUGHPUT
    }
}
