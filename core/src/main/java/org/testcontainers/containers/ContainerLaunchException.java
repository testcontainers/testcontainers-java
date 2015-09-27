package org.testcontainers.containers;

/**
 * AN exception that may be raised during launch of a container.
 */
class ContainerLaunchException extends RuntimeException {

    public ContainerLaunchException(String message) {
        super(message);
    }

    public ContainerLaunchException(String message, Exception exception) {
        super(message, exception);
    }
}
