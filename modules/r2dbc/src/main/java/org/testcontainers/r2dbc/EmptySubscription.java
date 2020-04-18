package org.testcontainers.r2dbc;

import org.reactivestreams.Subscription;

enum EmptySubscription implements Subscription {
    INSTANCE;

    @Override
    public void request(long n) {

    }

    @Override
    public void cancel() {

    }
}
