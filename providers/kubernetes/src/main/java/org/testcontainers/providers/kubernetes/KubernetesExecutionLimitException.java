package org.testcontainers.providers.kubernetes;

public class KubernetesExecutionLimitException extends Exception {
    public KubernetesExecutionLimitException() {
    }

    public KubernetesExecutionLimitException(String message) {
        super(message);
    }

    public KubernetesExecutionLimitException(String message, Throwable cause) {
        super(message, cause);
    }

    public KubernetesExecutionLimitException(Throwable cause) {
        super(cause);
    }

    public KubernetesExecutionLimitException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }
}
