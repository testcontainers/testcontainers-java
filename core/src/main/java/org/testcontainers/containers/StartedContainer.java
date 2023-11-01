package org.testcontainers.containers;

import org.testcontainers.UnstableAPI;

@UnstableAPI
interface StartedContainer extends ContainerState, AutoCloseable {
    @Override
    void close();
}
