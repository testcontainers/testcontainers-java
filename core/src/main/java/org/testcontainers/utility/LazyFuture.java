package org.testcontainers.utility;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Delegate;
import org.rnorth.ducttape.timeouts.Timeouts;

import java.util.concurrent.*;

/**
 * Future implementation with lazy result evaluation <b>in the same Thread</b> as caller.
 *
 * @param <T>
 */
public abstract class LazyFuture<T> implements Future<T> {

    @Delegate(excludes = Excludes.class)
    private final Future<T> delegate = CompletableFuture.completedFuture(null);

    @Getter(value = AccessLevel.MODULE, lazy = true)
    private final T resolvedValue = resolve();

    abstract protected T resolve();

    @Override
    public T get() throws InterruptedException, ExecutionException {
        return getResolvedValue();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        try {
            return Timeouts.getWithTimeout((int) timeout, unit, this::get);
        } catch (org.rnorth.ducttape.TimeoutException e) {
            throw new TimeoutException(e.getMessage());
        }
    }

    private interface Excludes<T> {
        T get();

        T get(long timeout, TimeUnit unit);
    }
}
