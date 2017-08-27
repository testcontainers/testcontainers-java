package org.testcontainers.dockerclient;

/**
 * Exception to indicate that a {@link DockerClientProviderStrategy} fails.
 */
public class InvalidConfigurationException extends RuntimeException {

    public InvalidConfigurationException(String s) {
        super(s);
    }

    public InvalidConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}