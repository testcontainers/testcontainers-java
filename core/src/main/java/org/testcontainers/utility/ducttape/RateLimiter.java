package org.testcontainers.utility.ducttape;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;

/**
 * Base class for rate limiters. Use RateLimiterBuilder to build new instances.
 * This code comes from <a href="https://github.com/rnorth/duct-tape/">rnorth/duct-tape</a>
 */
public abstract class RateLimiter {

    protected long lastInvocation;

    /**
     * Invoke a lambda function, with Thread.sleep() being called to limit the execution rate if needed.
     * @param lambda a Runnable lamda function to invoke
     */
    public void doWhenReady(@NotNull final Runnable lambda) {

        // Wait before proceeding, if needed
        long waitBeforeNextInvocation = getWaitBeforeNextInvocation();
        try {
            Thread.sleep(waitBeforeNextInvocation);
        } catch (InterruptedException ignored) { }

        try {
            lambda.run();
        } finally {
            lastInvocation = System.currentTimeMillis();
        }
    }

    /**
     *
     * Invoke a lambda function and get the result, with Thread.sleep() being called to limit the execution rate
     * if needed.
     * @param lambda a Callable lamda function to invoke
     * @param <T> return type of the lamda
     * @throws Exception rethrown from lambda
     * @return result of the lambda call
     */
    public <T> T getWhenReady(@NotNull final Callable<T> lambda) throws Exception {

        // Wait before proceeding, if needed
        long waitBeforeNextInvocation = getWaitBeforeNextInvocation();
        try {
            Thread.sleep(waitBeforeNextInvocation);
        } catch (InterruptedException ignored) { }

        try {
            return lambda.call();
        } finally {
            lastInvocation = System.currentTimeMillis();
        }
    }

    protected abstract long getWaitBeforeNextInvocation();
}
