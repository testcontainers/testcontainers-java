package org.testcontainers.controller;

public class NoSuchProviderException extends Exception {
    public NoSuchProviderException() {
    }

    public NoSuchProviderException(String message) {
        super(message);
    }

    public NoSuchProviderException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoSuchProviderException(Throwable cause) {
        super(cause);
    }

    public NoSuchProviderException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
