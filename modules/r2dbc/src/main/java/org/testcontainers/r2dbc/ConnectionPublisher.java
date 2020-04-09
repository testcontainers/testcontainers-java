package org.testcontainers.r2dbc;

import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactory;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Design notes:
 * - ConnectionPublisher is Mono-like (0..1), the request amount is ignored
 * - given the testing nature, the performance requirements are less strict
 * - "synchronized" is used to avoid races
 * - Reactive Streams spec violations are not checked (e.g. non-positive request)
 */
class ConnectionPublisher implements Publisher<Connection> {

    private final Supplier<CompletableFuture<ConnectionFactory>> futureSupplier;

    ConnectionPublisher(Supplier<CompletableFuture<ConnectionFactory>> futureSupplier) {
        this.futureSupplier = futureSupplier;
    }

    @Override
    public void subscribe(Subscriber<? super Connection> actual) {
        actual.onSubscribe(new StateMachineSubscription(actual));
    }

    private class StateMachineSubscription implements Subscription {

        private final Subscriber<? super Connection> actual;

        Subscription subscriptionState;

        StateMachineSubscription(Subscriber<? super Connection> actual) {
            this.actual = actual;
            subscriptionState = new WaitRequestSubscriptionState();
        }

        @Override
        public synchronized void request(long n) {
            subscriptionState.request(n);
        }

        @Override
        public synchronized void cancel() {
            subscriptionState.cancel();
        }

        synchronized void transitionTo(SubscriptionState newState) {
            subscriptionState = newState;
            newState.enter();
        }

        abstract class SubscriptionState implements Subscription {
            void enter() {
            }
        }

        class WaitRequestSubscriptionState extends SubscriptionState {

            @Override
            public void request(long n) {
                transitionTo(new WaitFutureCompletionSubscriptionState());
            }

            @Override
            public void cancel() {
            }
        }

        class WaitFutureCompletionSubscriptionState extends SubscriptionState {

            private CompletableFuture<ConnectionFactory> future;

            @Override
            void enter() {
                this.future = futureSupplier.get();

                future.whenComplete((connectionFactory, e) -> {
                    if (e != null) {
                        actual.onSubscribe(EmptySubscription.INSTANCE);
                        actual.onError(e);
                        return;
                    }

                    Publisher<? extends Connection> publisher = connectionFactory.create();
                    transitionTo(new ProxySubscriptionState(publisher));
                });
            }

            @Override
            public void request(long n) {
            }

            @Override
            public void cancel() {
                future.cancel(true);
            }
        }

        class ProxySubscriptionState extends SubscriptionState implements Subscriber<Connection> {

            private final Publisher<? extends Connection> publisher;

            private Subscription s;

            private boolean cancelled = false;

            ProxySubscriptionState(Publisher<? extends Connection> publisher) {
                this.publisher = publisher;
            }

            @Override
            void enter() {
                publisher.subscribe(this);
            }

            @Override
            public void request(long n) {
                // Ignore
            }

            @Override
            public synchronized void cancel() {
                cancelled = true;
                if (s != null) {
                    s.cancel();
                }
            }

            @Override
            public synchronized void onSubscribe(Subscription s) {
                this.s = s;
                if (!cancelled) {
                    s.request(1);
                } else {
                    s.cancel();
                }
            }

            @Override
            public void onNext(Connection connection) {
                actual.onNext(connection);
            }

            @Override
            public void onError(Throwable t) {
                actual.onError(t);
            }

            @Override
            public void onComplete() {
                actual.onComplete();
            }
        }
    }
}
