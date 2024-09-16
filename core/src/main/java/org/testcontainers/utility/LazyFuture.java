package org.testcontainers.utility;

import lombok.AccessLevel;
import lombok.Getter;
import org.testcontainers.utility.ducttape.Timeouts;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Future implementation with lazy result evaluation <b>in the same Thread</b> as caller.
 *
 * @param <T>
 */
public abstract class LazyFuture<T> implements Future<T> {

    @Getter(value = AccessLevel.MODULE, lazy = true)
    private final T resolvedValue = resolve();

    protected abstract T resolve();

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return ((AtomicReference<?>) resolvedValue).get() != null;
    }

    @Override
    public T get() {
        return getResolvedValue();
    }

    @Override
    public T get(long timeout, TimeUnit unit) throws TimeoutException {
        try {
            return Timeouts.getWithTimeout((int) timeout, unit, this::get);
        } catch (org.testcontainers.utility.ducttape.TimeoutException e) {
            throw new TimeoutException(e.getMessage());
        }
    }
}
