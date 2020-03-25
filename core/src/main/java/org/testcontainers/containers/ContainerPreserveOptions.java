package org.testcontainers.containers;

/**
 * Enum specifying conditions on which container instance will not be deleted from docker daemon.
 */
public enum ContainerPreserveOptions {
    ON_JVM_SHUTDOWN, ON_TESTS_FAILURE
}
