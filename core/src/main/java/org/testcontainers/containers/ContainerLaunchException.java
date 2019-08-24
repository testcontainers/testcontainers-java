package org.testcontainers.containers;

/**
 * AN exception that may be raised during launch of a container.
 */
public class ContainerLaunchException extends RuntimeException {

    public ContainerLaunchException(String message) {
        super(message);
    }

    public ContainerLaunchException(String message, Throwable cause) {
        super(message, cause);
    }
}
