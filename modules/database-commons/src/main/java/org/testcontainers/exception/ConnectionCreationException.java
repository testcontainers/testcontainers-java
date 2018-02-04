package org.testcontainers.exception;

/**
 * Inability to create connection to the database
 *
 * @author Eugeny Karpov
 */
public class ConnectionCreationException extends RuntimeException {

    public ConnectionCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
