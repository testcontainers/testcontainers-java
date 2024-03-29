package org.testcontainers.containers;

interface ContainerLifecycleHooks {
    ContainerLifecycleHooks EMPTY = new ContainerLifecycleHooks() {};

    default void configure() {}

    default void containerIsStarting(boolean reused) {}

    default void containerIsStarted(boolean reused) {}
}
