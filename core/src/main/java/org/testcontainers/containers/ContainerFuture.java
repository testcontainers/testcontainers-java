package org.testcontainers.containers;

import java.util.concurrent.CompletionStage;
import java.util.concurrent.Future;

interface ContainerFuture<S extends StartedContainer> extends Future<S>, CompletionStage<S>, AutoCloseable {
    @Override
    void close();
}
