package org.testcontainers.containers;

import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.testcontainers.lifecycle.Startable;

import java.util.concurrent.CompletableFuture;

@RequiredArgsConstructor
class ContainerFutureImpl<S extends StartedContainer> implements ContainerFuture<S> {

    @Delegate
    private final CompletableFuture<S> delegate;

    private final Startable startable;

    public void cancel() {
        delegate.cancel(true);
        startable.stop();
    }

    @Override
    public void close() {
        cancel();
    }
}
