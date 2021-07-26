package org.testcontainers.r2dbc;

import io.r2dbc.spi.Closeable;
import io.r2dbc.spi.Connection;
import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.reactivestreams.Publisher;
import org.testcontainers.lifecycle.Startable;

import java.util.ServiceLoader;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.StreamSupport;

class TestcontainersR2DBCConnectionFactory implements ConnectionFactory, Closeable {

    private static final AtomicLong THREAD_COUNT = new AtomicLong();

    private static final Executor EXECUTOR = Executors.newCachedThreadPool(r -> {
        Thread thread = new Thread(r);
        thread.setName("testcontainers-r2dbc-" + THREAD_COUNT.getAndIncrement());
        thread.setDaemon(true);
        return thread;
    });

    private final ConnectionFactoryOptions options;

    private final R2DBCDatabaseContainerProvider containerProvider;

    private CompletableFuture<R2DBCDatabaseContainer> future;

    TestcontainersR2DBCConnectionFactory(ConnectionFactoryOptions options) {
        this.options = options;

        containerProvider = StreamSupport.stream(ServiceLoader.load(R2DBCDatabaseContainerProvider.class).spliterator(), false)
            .filter(it -> it.supports(options))
            .findAny()
            .orElseThrow(() -> new IllegalArgumentException("Missing provider for " + options));
    }

    @Override
    public Publisher<? extends Connection> create() {
        return new ConnectionPublisher(
            () -> {
                if (future == null) {
                    synchronized (this) {
                        if (future == null) {
                            future = CompletableFuture.supplyAsync(() -> {
                                R2DBCDatabaseContainer container = containerProvider.createContainer(options);
                                container.start();
                                return container;
                            }, EXECUTOR);
                        }
                    }
                }
                return future.thenApply(it -> {
                    return ConnectionFactories.find(
                        it.configure(options)
                    );
                });
            }
        );
    }

    @Override
    public ConnectionFactoryMetadata getMetadata() {
        return containerProvider.getMetadata(options);
    }

    @Override
    public Publisher<Void> close() {
        return s -> {
            CompletableFuture<R2DBCDatabaseContainer> futureRef;
            synchronized (this) {
                futureRef = this.future;
                this.future = null;
            }

            CancellableSubscription subscription = new CancellableSubscription();
            s.onSubscribe(subscription);

            if (futureRef == null) {
                if (!subscription.isCancelled()) {
                    s.onComplete();
                }
            } else {
                futureRef.thenAcceptAsync(Startable::stop, EXECUTOR);

                EXECUTOR.execute(() -> {
                    futureRef.cancel(true);
                    if (!subscription.isCancelled()) {
                        s.onComplete();
                    }
                });
            }
        };
    }

}
