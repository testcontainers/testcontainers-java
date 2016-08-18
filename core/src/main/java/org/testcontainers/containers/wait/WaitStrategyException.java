package org.testcontainers.containers.wait;

/**
 * Created by qoomon on 25.06.16.
 */
public class WaitStrategyException extends RuntimeException {

    public WaitStrategyException(String message) {
        super(message);
    }

    public WaitStrategyException(String message, Throwable cause) {
        super(message, cause);
    }
}
