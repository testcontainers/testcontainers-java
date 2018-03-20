package org.testcontainers.containers;

public class ContainerNotStartedException extends RuntimeException {

    public ContainerNotStartedException(String message) {
        super(message);
    }
}
