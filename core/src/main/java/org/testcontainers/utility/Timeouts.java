package org.testcontainers.utility;

import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Utility methods for enforcing timeouts on operations using resilience4j TimeLimiter.
 */
public final class Timeouts {

    private Timeouts() {}

    /**
     * Executes the given callable with a timeout, returning its result.
     *
     * @param timeout the timeout duration
     * @param unit the time unit of the timeout
     * @param callable the operation to execute
     * @param <T> the return type
     * @return the result of the callable
     * @throws TimeoutException if the operation times out
     */
    public static <T> T getWithTimeout(int timeout, TimeUnit unit, Callable<T> callable) {
        if (timeout <= 0) {
            throw new TimeoutException("Timeout must be positive, was: " + timeout);
        }
        TimeLimiterConfig config = TimeLimiterConfig
            .custom()
            .timeoutDuration(Duration.ofMillis(unit.toMillis(timeout)))
            .cancelRunningFuture(true)
            .build();
        TimeLimiter timeLimiter = TimeLimiter.of(config);
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "testcontainers-timeout");
            t.setDaemon(true);
            return t;
        });
        try {
            Callable<T> decorated = TimeLimiter.decorateFutureSupplier(timeLimiter, () -> executor.submit(callable));
            return decorated.call();
        } catch (java.util.concurrent.TimeoutException e) {
            throw new TimeoutException("Timeout after " + timeout + " " + unit, e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw new RuntimeException(cause);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new TimeoutException("Interrupted", e);
        } catch (Exception e) {
            if (e instanceof RuntimeException) {
                throw (RuntimeException) e;
            }
            throw new RuntimeException(e);
        } finally {
            executor.shutdownNow();
        }
    }

    /**
     * Executes the given runnable with a timeout.
     *
     * @param timeout the timeout duration
     * @param unit the time unit of the timeout
     * @param runnable the operation to execute
     * @throws TimeoutException if the operation times out
     */
    public static void doWithTimeout(int timeout, TimeUnit unit, Runnable runnable) {
        getWithTimeout(
            timeout,
            unit,
            () -> {
                runnable.run();
                return null;
            }
        );
    }
}
