package org.testcontainers.utility.ducttape;

/**
 * Indicates repeated failure of an UnreliableSupplier
 * This code comes from <a href="https://github.com/rnorth/duct-tape/">rnorth/duct-tape</a>
 */
public class RetryCountExceededException extends RuntimeException {

    public RetryCountExceededException(String message, Exception exception) {
        super(message, exception);
    }
}
