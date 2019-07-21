package org.testcontainers.lifecycle;

import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
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
        return deepStart(new HashMap<>(), startables);
    }

    private CompletableFuture<Void> deepStart(Map<Startable, CompletableFuture<Void>> started, Stream<Startable> startables) {
        CompletableFuture[] futures = startables
            .map(it -> {
                // avoid a recursive update in `computeIfAbsent`
                Map<Startable, CompletableFuture<Void>> subStarted = new HashMap<>(started);
                CompletableFuture<Void> future = started.computeIfAbsent(it, startable -> {
                    return deepStart(subStarted, startable.getDependencies().stream()).thenRunAsync(startable::start, EXECUTOR);
                });
                started.putAll(subStarted);
                return future;
            })
            .toArray(CompletableFuture[]::new);

        return CompletableFuture.allOf(futures);
    }
}
