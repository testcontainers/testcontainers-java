package org.testcontainers.exception;

/**
 * Inability to create connection to the database
 */
public class ConnectionCreationException extends RuntimeException {

    public ConnectionCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
