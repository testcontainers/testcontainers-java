package org.testcontainers.containers.wait.internal;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.ExecInContainerPattern;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.String.format;
import static java.util.concurrent.Executors.*;
import static org.testcontainers.containers.wait.internal.ThreadFactories.prefixedThreadFactory;

/**
 * Mechanism for testing that a socket is listening when run from the container being checked.
 */
@RequiredArgsConstructor
@Slf4j
public class InternalCommandPortListeningCheck implements AsyncCheck {

    private final WaitStrategyTarget waitStrategyTarget;
    private final Set<Integer> internalPorts;

    @Override
    public CompletableFuture<Boolean> perform() {

        ExecutorService executor = newSingleThreadExecutor(prefixedThreadFactory("internal-command-port-listening-check"));

        return CompletableFuture.supplyAsync(() -> {
            String command = "true";

            for (int internalPort : internalPorts) {
                command += " && ";
                command += " (";
                command += format("cat /proc/net/tcp{,6} | awk '{print $2}' | grep -i :%x", internalPort);
                command += " || ";
                command += format("nc -vz -w 1 localhost %d", internalPort);
                command += " || ";
                command += format("/bin/bash -c '</dev/tcp/localhost/%d'", internalPort);
                command += ")";
            }
            Instant before = Instant.now();
            try {
                ExecResult result = ExecInContainerPattern.execInContainer(waitStrategyTarget.getContainerInfo(), "/bin/sh", "-c", command);
                log.trace("Check for {} took {}", internalPorts, Duration.between(before, Instant.now()));
                return result.getExitCode() == 0;
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }, executor);
    }
}
