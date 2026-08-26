package org.testcontainers.utility;

import io.github.resilience4j.ratelimiter.RateLimiterConfig;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Builder for {@link RateLimiter} instances. API mirrors the historical ducttape
 * {@code RateLimiterBuilder} so existing call patterns continue to compile.
 * Implementations delegate to resilience4j internally.
 */
public final class RateLimiterBuilder {

    private int rate = 1;

    private TimeUnit perTimeUnit = TimeUnit.SECONDS;

    private boolean strategySet;

    private RateLimiterBuilder() {}

    public static RateLimiterBuilder newBuilder() {
        return new RateLimiterBuilder();
    }

    public RateLimiterBuilder withRate(int rate, TimeUnit perTimeUnit) {
        this.rate = rate;
        this.perTimeUnit = perTimeUnit;
        return this;
    }

    public RateLimiterBuilder withConstantThroughput() {
        this.strategySet = true;
        return this;
    }

    public RateLimiter build() {
        if (!strategySet) {
            throw new IllegalStateException("A rate limiter strategy must be set (e.g. withConstantThroughput())");
        }
        RateLimiterConfig config = RateLimiterConfig
            .custom()
            .limitForPeriod(rate)
            .limitRefreshPeriod(Duration.ofNanos(perTimeUnit.toNanos(1)))
            .timeoutDuration(Duration.ofHours(1))
            .build();
        io.github.resilience4j.ratelimiter.RateLimiter delegate = io.github.resilience4j.ratelimiter.RateLimiter.of(
            "tc-rate-limiter-" + rate + "-per-" + perTimeUnit.name().toLowerCase(),
            config
        );
        return new Resilience4jAdapter(delegate);
    }

    private static final class Resilience4jAdapter implements RateLimiter {

        private final io.github.resilience4j.ratelimiter.RateLimiter delegate;

        Resilience4jAdapter(io.github.resilience4j.ratelimiter.RateLimiter delegate) {
            this.delegate = delegate;
        }

        @Override
        public void doWhenReady(Runnable runnable) {
            delegate.executeRunnable(runnable);
        }

        @Override
        public <T> T getWhenReady(Callable<T> callable) throws Exception {
            return io.github.resilience4j.ratelimiter.RateLimiter.decorateCallable(delegate, callable).call();
        }
    }
}
