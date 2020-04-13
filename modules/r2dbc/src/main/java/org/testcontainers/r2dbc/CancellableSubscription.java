package org.testcontainers.r2dbc;

import org.reactivestreams.Subscription;

import java.util.concurrent.atomic.AtomicBoolean;

class CancellableSubscription implements Subscription {

    private final AtomicBoolean cancelled = new AtomicBoolean();

    @Override
    public void request(long n) {
    }

    @Override
    public void cancel() {
        cancelled.set(true);
    }

    public boolean isCancelled() {
        return cancelled.get();
    }
}
