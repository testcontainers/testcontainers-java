package org.testcontainers.utility.ducttape;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Utilities to time out on slow running code.
 * This code comes from <a href="https://github.com/rnorth/duct-tape/">rnorth/duct-tape</a>
 */
public class Timeouts {

    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger(0);

    private static final ThreadFactory THREAD_FACTORY = r -> {
        Thread thread = new Thread(r, "ducttape-" + THREAD_COUNTER.getAndIncrement());
        thread.setDaemon(true);
        return thread;
    };

    private static volatile ExecutorService executorService;

    private static synchronized ExecutorService getExecutorService() {
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newCachedThreadPool(THREAD_FACTORY);
        }
        return executorService;
    }

    public static synchronized void shutdown() {
        if (executorService != null) {
            executorService.shutdown();
            executorService = null;
        }
    }

    /**
     * Execute a lambda expression with a timeout. If it completes within the time, the result will be returned.
     * If it does not complete within the time, a TimeoutException will be thrown.
     * If it throws an exception, a RuntimeException wrapping that exception will be thrown.
     *
     * @param timeout  how long to wait
     * @param timeUnit time unit for time interval
     * @param lambda   supplier lambda expression (may throw checked exceptions)
     * @param <T>      return type of the lambda
     * @return the result of the successful lambda expression call
     */
    public static <T> T getWithTimeout(final int timeout, final TimeUnit timeUnit, final Callable<T> lambda) {

        check("timeout must be greater than zero", timeout > 0);

        Future<T> future = getExecutorService().submit(lambda);
        return callFuture(timeout, timeUnit, future);
    }

    /**
     * Execute a lambda expression with a timeout. If it completes within the time, the result will be returned.
     * If it does not complete within the time, a TimeoutException will be thrown.
     * If it throws an exception, a RuntimeException wrapping that exception will be thrown.
     *
     * @param timeout  how long to wait
     * @param timeUnit time unit for time interval
     * @param lambda   supplier lambda expression (may throw checked exceptions)
     */
    public static void doWithTimeout(final int timeout, final TimeUnit timeUnit, final Runnable lambda) {

        check("timeout must be greater than zero", timeout > 0);

        Future<?> future = getExecutorService().submit(lambda);
        callFuture(timeout, timeUnit, future);
    }

    private static <T> T callFuture(final int timeout, final TimeUnit timeUnit, final Future<T> future) {
        try {
            return future.get(timeout, timeUnit);
        } catch (ExecutionException e) {
            // The cause of the ExecutionException is the actual exception that was thrown
            throw new RuntimeException(e.getCause());
        } catch (java.util.concurrent.TimeoutException | InterruptedException e) {
            throw new TimeoutException(e);
        }
    }

    private static void check(String message, boolean condition) {
        if (!condition) {
            throw new IllegalArgumentException("Precondition failed: " + message);
        }
    }
}
