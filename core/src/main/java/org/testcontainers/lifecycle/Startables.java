package org.testcontainers.lifecycle;

import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

@UtilityClass
public class Startables {

    private static final Executor EXECUTOR = Executors.newCachedThreadPool(new ThreadFactory() {

        private final AtomicLong COUNTER = new AtomicLong(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "testcontainers-lifecycle-" + COUNTER.getAndIncrement());
            thread.setDaemon(true);
            return thread;
        }
    });

    public CompletableFuture<Void> deepStart(Collection<Startable> startables) {
        return deepStart(startables.stream());
    }

    public CompletableFuture<Void> deepStart(Stream<Startable> startables) {
        return deepStart(new ConcurrentHashMap<>(), startables);
    }

    private CompletableFuture<Void> deepStart(ConcurrentMap<Startable, CompletableFuture<Void>> started, Stream<Startable> startables) {
        return CompletableFuture.allOf(
            startables
                .map(it -> started.computeIfAbsent(it, startable -> {
                    return deepStart(started, startable.getDependencies().stream()).thenRunAsync(startable::start, EXECUTOR);
                }))
                .toArray(CompletableFuture[]::new)
        );
    }
}
