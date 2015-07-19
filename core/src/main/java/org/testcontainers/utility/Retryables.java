package org.testcontainers.utility;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Utilities to support automatic retry of things that may tend to not always produce consistent results.
 */
public class Retryables {

    private static final Logger LOGGER = LoggerFactory.getLogger(Retryables.class);

    /**
     * Call a supplier repeatedly until it returns a result. If an exception is thrown, the call
     * will be retried repeatedly until the timeout is hit.
     *
     * @param timeout   how long to wait
     * @param timeUnit  how long to wait (units)
     * @param lambda    supplier lambda expression (may throw checked exceptions)
     * @param <T>       return type of the supplier
     * @return          the
     */
    public static <T> T retryUntilSuccess(final int timeout, final TimeUnit timeUnit, final UnreliableSupplier<T> lambda) {
        long timeLimit = TimeUnit.MILLISECONDS.convert(timeout, timeUnit) + System.currentTimeMillis();

        int attempt = 0;
        Exception lastException = null;
        while (System.currentTimeMillis() < timeLimit) {

            try {
                return lambda.get();
            } catch (Exception e) {
                // Failed
                LOGGER.trace("Retrying lambda call on attempt {}", attempt++);
                lastException = e;

            } finally {
                // always sleep 200ms each time through the loop, even if the result was successful
                // this enables a fractional amount more time for the result to stabilize
                try {
                    Thread.sleep(200L);
                } catch (InterruptedException ignored) { }
            }
        }

        throw new TimeoutException("Timeout waiting for result", lastException);
    }

    /**
     * Call a callable repeatedly until it returns true. If an exception is thrown, the call
     * will be retried repeatedly until the timeout is hit.
     *
     * @param timeout   how long to wait
     * @param timeUnit  how long to wait (units)
     * @param lambda    supplier lambda expression
     */
    public static void retryUntilTrue(final int timeout, final TimeUnit timeUnit, final Callable<Boolean> lambda) {
        retryUntilSuccess(timeout, timeUnit, new UnreliableSupplier<Object>() {
            @Override
            public Object get() throws Exception {
                if (!lambda.call()) {
                    throw new RuntimeException("Not ready yet");
                } else {
                    return null;
                }
            }
        });
    }

    /**
     * A variation on java.util.function.Supplier which allows checked exceptions to be thrown.
     * @param <T>
     */
    public interface UnreliableSupplier<T> {
        T get() throws Exception;
    }

    /**
     * Indicates timeout of an UnreliableSupplier
     */
    public static class TimeoutException extends RuntimeException {

        public TimeoutException(@NotNull String message, @Nullable Exception exception) {
            super(message, exception);
        }
    }
}
