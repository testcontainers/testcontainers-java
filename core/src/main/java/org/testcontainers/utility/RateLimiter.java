package org.testcontainers.utility;

import java.util.concurrent.Callable;

/**
 * Throttles operations to a configured rate. Created via {@link RateLimiterBuilder}.
 * <p>
 * Method names match the historical ducttape API so callers that consumed the
 * rate limiter via {@link org.testcontainers.containers.wait.strategy.AbstractWaitStrategy#getRateLimiter()}
 * compile unchanged.
 */
public interface RateLimiter {
    /**
     * Runs the runnable after acquiring permission. Blocks if rate exceeded.
     */
    void doWhenReady(Runnable runnable);

    /**
     * Calls the callable after acquiring permission and returns its result.
     * Blocks if rate exceeded.
     */
    <T> T getWhenReady(Callable<T> callable) throws Exception;
}
