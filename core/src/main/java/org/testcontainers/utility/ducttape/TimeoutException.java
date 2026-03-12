package org.testcontainers.utility.ducttape;

/**
 * Indicates timeout of an UnreliableSupplier
 * This code comes from <a href="https://github.com/rnorth/duct-tape/">rnorth/duct-tape</a>
 */
public class TimeoutException extends RuntimeException {

    public TimeoutException(String message, Exception exception) {
        super(message, exception);
    }

    public TimeoutException(Exception e) {
        super(e);
    }
}
