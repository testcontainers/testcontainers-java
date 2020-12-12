package org.testcontainers.lifecycle;

import java.util.Arrays;
import lombok.experimental.UtilityClass;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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

    /**
     * @see #deepStart(Stream)
     */
    public CompletableFuture<Void> deepStart(Collection<? extends Startable> startables) {
        return deepStart((Iterable<? extends Startable>) startables);
    }

    /**
     * @see #deepStart(Stream)
     */
    public CompletableFuture<Void> deepStart(Iterable<? extends Startable> startables) {
        return deepStart(StreamSupport.stream(startables.spliterator(), false));
    }

    /**
     * @see #deepStart(Stream)
     */
    public CompletableFuture<Void> deepStart(Startable... startables) {
        return deepStart(Arrays.stream(startables));
    }

    /**
     * Start every {@link Startable} recursively and asynchronously and join on the result.
     *
     * Performance note:
     * The method uses and returns {@link CompletableFuture}s to resolve as many {@link Startable}s at once as possible.
     * This way, for the following graph:
     *   / b \
     * a      e
     *     c /
     *     d /
     * "a", "c" and "d" will resolve in parallel, then "b".
     *
     * If we would call blocking {@link Startable#start()}, "e" would wait for "b", "b" for "a", and only then "c", and then "d".
     * But, since "c" and "d" are independent from "a", there is no point in waiting for "a" to be resolved first.
     *
     * @param startables a {@link Stream} of {@link Startable}s to start and scan for transitive dependencies.
     * @return a {@link CompletableFuture} that resolves once all {@link Startable}s have started.
     */
    public CompletableFuture<Void> deepStart(Stream<? extends Startable> startables) {
        return deepStart(new HashMap<>(), startables);
    }

    /**
     *
     * @param started an intermediate storage for already started {@link Startable}s to prevent multiple starts.
     * @param startables a {@link Stream} of {@link Startable}s to start and scan for transitive dependencies.
     */
    private CompletableFuture<Void> deepStart(Map<Startable, CompletableFuture<Void>> started, Stream<? extends Startable> startables) {
        CompletableFuture[] futures = startables
            .sequential()
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
