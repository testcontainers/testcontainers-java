package org.testcontainers.containers.wait.internal;

import lombok.RequiredArgsConstructor;
import org.testcontainers.containers.ContainerState;

import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;
import static org.testcontainers.containers.wait.internal.ThreadFactories.prefixedThreadFactory;

/**
 * Mechanism for testing that a socket is listening when run from the test host.
 */
@RequiredArgsConstructor
public class ExternalPortListeningCheck implements AsyncCheck {
    private final ContainerState containerState;
    private final Set<Integer> externalLivenessCheckPorts;

    @Override
    public CompletableFuture<Boolean> perform() {
        ExecutorService executor = Executors.newFixedThreadPool(Math.min(externalLivenessCheckPorts.size(), 100), prefixedThreadFactory("external-port-listening-check"));

        return externalLivenessCheckPorts.stream()
            .map(externalPort -> openSocketAsync(containerState.getContainerIpAddress(), externalPort, executor))
            .collect(collectingAndThen(toList(), ExternalPortListeningCheck::combineOrShortcircuit))
            .thenApply(__ -> true);
    }

    private static CompletableFuture<Void> openSocketAsync(String address, Integer externalPort, ExecutorService executor) {
        return CompletableFuture.runAsync(() -> {
            try {
                new Socket(address, externalPort).close();
            } catch (IOException e) {
                throw new IllegalStateException("Socket not listening yet: " + externalPort);
            }
        }, executor);
    }

    /**
     * Short-circuiting version of {@link CompletableFuture#allOf(CompletableFuture[])}
     */
    private static <T> CompletableFuture<Void> combineOrShortcircuit(List<CompletableFuture<T>> futures) {
        CompletableFuture<Void> result = new CompletableFuture<>();

        for (CompletableFuture<?> f : futures) {
            f.handle((__, ex) -> ex != null && result.completeExceptionally(ex));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .handle((__, ex) -> ex != null ? result.completeExceptionally(ex) : result.complete(null));

        return result;
    }
}
