package org.testcontainers.containers.wait.strategy;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Awaitility;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.wait.internal.ExternalPortListeningCheck;
import org.testcontainers.containers.wait.internal.InternalCommandPortListeningCheck;

import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

/**
 * Waits until a socket connection can be established on a port exposed or mapped by the container.
 *
 * @author richardnorth
 */
@Slf4j
public class HostPortWaitStrategy extends AbstractWaitStrategy {

    @Override
    @SneakyThrows(InterruptedException.class)
    protected void waitUntilReady() {
        final Set<Integer> externalLivenessCheckPorts = getLivenessCheckPorts();
        if (externalLivenessCheckPorts.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("Liveness check ports of {} is empty. Not waiting.", waitStrategyTarget.getContainerInfo().getName());
            }
            return;
        }

        List<Integer> exposedPorts = waitStrategyTarget.getExposedPorts();

        final Set<Integer> internalPorts = getInternalPorts(externalLivenessCheckPorts, exposedPorts);

        Callable<Boolean> internalCheck = new InternalCommandPortListeningCheck(waitStrategyTarget, internalPorts);

        Callable<Boolean> externalCheck = new ExternalPortListeningCheck(waitStrategyTarget, externalLivenessCheckPorts);

        try {
            List<Future<Boolean>> futures = EXECUTOR.invokeAll(Arrays.asList(
                // Blocking
                () -> {
                    Instant now = Instant.now();
                    Boolean result = internalCheck.call();
                    log.debug(
                        "Internal port check {} for {} in {}",
                        Boolean.TRUE.equals(result) ? "passed" : "failed",
                        internalPorts,
                        Duration.between(now, Instant.now())
                    );
                    return result;
                },
                // Polling
                () -> {
                    Instant now = Instant.now();
                    Awaitility.await()
                        .pollInSameThread()
                        .pollInterval(Duration.ofMillis(100))
                        .pollDelay(Duration.ZERO)
                        .ignoreExceptions()
                        .forever()
                        .until(externalCheck);

                    log.debug(
                        "External port check passed for {} mapped as {} in {}",
                        internalPorts,
                        externalLivenessCheckPorts,
                        Duration.between(now, Instant.now())
                    );
                    return true;
                }
            ), startupTimeout.getSeconds(), TimeUnit.SECONDS);

            for (Future<Boolean> future : futures) {
                future.get(0, TimeUnit.SECONDS);
            }
        } catch (CancellationException | ExecutionException | TimeoutException e) {
            throw new ContainerLaunchException("Timed out waiting for container port to open (" +
                    waitStrategyTarget.getHost() +
                    " ports: " +
                    externalLivenessCheckPorts +
                    " should be listening)");
        }
    }

    private Set<Integer> getInternalPorts(Set<Integer> externalLivenessCheckPorts, List<Integer> exposedPorts) {
        return exposedPorts.stream()
                .filter(it -> externalLivenessCheckPorts.contains(waitStrategyTarget.getMappedPort(it)))
                .collect(Collectors.toSet());
    }
}
