package org.testcontainers.containers;

interface ContainerHooksAware {

    ContainerHooksAware DUMMY = new ContainerHooksAware() {
    };

    default void beforeStart() {

    }

    default void containerIsCreated(String containerId) {

    }

    default void containerIsStarting(boolean reused) {

    }

    default void containerIsStarted(boolean reused) {

    }

    default void containerIsStopping() {

    }

    default void containerIsStopped() {

    }
}
