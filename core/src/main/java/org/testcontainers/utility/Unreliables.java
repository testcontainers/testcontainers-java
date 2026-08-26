package org.testcontainers.utility;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Retry utilities for unreliable operations.
 */
public final class Unreliables {

    private Unreliables() {}

    /**
     * Retries the given callable until it succeeds or the timeout is reached.
     * The entire retry loop is executed within {@link Timeouts#getWithTimeout} so that
     * a blocking callable cannot hang beyond the deadline.
     *
     * @param timeout the timeout value (must be positive)
     * @param unit the time unit of the timeout
     * @param callable the operation to retry
     * @param <T> the return type
     * @return the result of the callable
     * @throws TimeoutException if the timeout is reached or the thread is interrupted
     */
    public static <T> T retryUntilSuccess(int timeout, TimeUnit unit, Callable<T> callable) {
        if (timeout <= 0) {
            throw new IllegalArgumentException("timeout must be positive, was: " + timeout);
        }
        AtomicReference<Exception> lastException = new AtomicReference<>();
        try {
            return Timeouts.getWithTimeout(
                timeout,
                unit,
                () -> {
                    while (!Thread.currentThread().isInterrupted()) {
                        try {
                            return callable.call();
                        } catch (Exception e) {
                            // Don't let an interrupt-induced exception overwrite the genuine
                            // last failure (e.g. an SSL handshake error) — that's what callers
                            // care about diagnosing.
                            if (!Thread.currentThread().isInterrupted()) {
                                lastException.set(e);
                            }
                        }
                    }
                    throw new TimeoutException("Interrupted", lastException.get());
                }
            );
        } catch (TimeoutException e) {
            Exception last = lastException.get();
            if (last != null) {
                throw new TimeoutException("Timeout waiting for result", last);
            }
            throw e;
        }
    }

    /**
     * Retries the given callable up to {@code tryLimit} times.
     *
     * @param tryLimit the maximum number of attempts
     * @param callable the operation to retry
     * @param <T> the return type
     * @return the result of the callable
     * @throws RetryCountExceededException if all attempts fail
     */
    public static <T> T retryUntilSuccess(int tryLimit, Callable<T> callable) {
        Exception lastException = null;
        for (int i = 0; i < tryLimit; i++) {
            try {
                return callable.call();
            } catch (Exception e) {
                lastException = e;
            }
        }
        throw new RetryCountExceededException("Retry limit hit with exception", lastException);
    }

    /**
     * Retries until the given callable returns {@code true} or the timeout is reached.
     *
     * @param timeout the timeout value
     * @param unit the time unit of the timeout
     * @param callable the condition to check
     * @throws TimeoutException if the timeout is reached
     */
    public static void retryUntilTrue(int timeout, TimeUnit unit, Callable<Boolean> callable) {
        retryUntilSuccess(
            timeout,
            unit,
            () -> {
                if (!Boolean.TRUE.equals(callable.call())) {
                    throw new RuntimeException("Not ready yet");
                }
                return true;
            }
        );
    }
}
