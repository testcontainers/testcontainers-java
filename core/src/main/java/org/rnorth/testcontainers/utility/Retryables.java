package org.rnorth.testcontainers.utility;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
 * Created by rnorth on 05/07/2015.
 */
public class Retryables {

    private static final Logger LOGGER = LoggerFactory.getLogger(Retryables.class);

    public static <T> T retryUntilSuccess(int timeout, TimeUnit timeUnit, UnreliableSupplier<T> lambda) {
        long timeLimit = TimeUnit.MILLISECONDS.convert(timeout, timeUnit) + System.currentTimeMillis();

        int attempt = 0;
        Exception lastException = null;
        while (System.currentTimeMillis() < timeLimit) {

            try {
                return lambda.get();
            } catch (Exception e) {
                // Failed
                LOGGER.debug("Retrying lambda call on attempt {}", attempt++);
                lastException = e;

            } finally {
                // always sleep 200ms each time through the loop, even if the result was successful
                // this enables a fractional amount more time for the result to stabilize
                try {
                    Thread.sleep(200L);
                } catch (InterruptedException ignored) { }
            }
        }

        throw new RuntimeException("Timeout waiting for result", lastException);
    }

    public interface UnreliableSupplier<T> {
        T get() throws Exception;
    }
}
