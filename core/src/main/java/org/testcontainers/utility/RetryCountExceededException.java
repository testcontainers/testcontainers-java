package org.testcontainers.utility;

/**
 * Exception thrown when the maximum number of retry attempts is exceeded.
 */
public class RetryCountExceededException extends RuntimeException {

    public RetryCountExceededException(String message) {
        super(message);
    }

    public RetryCountExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
