package org.testcontainers.controller;

public class UnsupportedProviderOperationException extends Exception {
    public UnsupportedProviderOperationException() {
    }

    public UnsupportedProviderOperationException(String message) {
        super(message);
    }

    public UnsupportedProviderOperationException(String message, Throwable cause) {
        super(message, cause);
    }

    public UnsupportedProviderOperationException(Throwable cause) {
        super(cause);
    }

    public UnsupportedProviderOperationException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
