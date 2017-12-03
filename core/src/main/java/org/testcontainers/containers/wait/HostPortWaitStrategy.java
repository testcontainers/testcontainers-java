package org.testcontainers.containers.wait;

import lombok.extern.slf4j.Slf4j;
import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.internal.ExternalPortListeningCheck;
import org.testcontainers.containers.wait.internal.InternalCommandPortListeningCheck;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Waits until a socket connection can be established on a port exposed or mapped by the container.
 *
 * @author richardnorth
 */
@Slf4j
public class HostPortWaitStrategy extends GenericContainer.AbstractWaitStrategy {

    @Override
    protected void waitUntilReady() {
        final Set<Integer> externalLivenessCheckPorts = getLivenessCheckPorts();
        if (externalLivenessCheckPorts.isEmpty()) {
            log.debug("Liveness check ports of {} is empty. Not waiting.", container.getContainerName());
            return;
        }

        List<Integer> exposedPorts = container.getExposedPorts();

        final Set<Integer> internalExposedPorts = exposedPorts.stream()
                .filter(it -> externalLivenessCheckPorts.contains(container.getMappedPort(it)))
                .collect(Collectors.toSet());

        final List<String> portBindings = container.getPortBindings();

        final Set<Integer> fixedPortBindingsInternalPorts = portBindings.stream()
                .map(pb -> pb.split(":")[1])
                .map(Integer::valueOf)
                .collect(Collectors.toSet());

        final HashSet<Integer> internalPorts = new HashSet<>();
        internalPorts.addAll(internalExposedPorts);
        internalPorts.addAll(fixedPortBindingsInternalPorts);

        Callable<Boolean> internalCheck = new InternalCommandPortListeningCheck(container, internalPorts);

        Callable<Boolean> externalCheck = new ExternalPortListeningCheck(container.getContainerIpAddress(), externalLivenessCheckPorts);

        try {
            Unreliables.retryUntilTrue((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> {
                return getRateLimiter().getWhenReady(() -> internalCheck.call() && externalCheck.call());
            });

        } catch (TimeoutException e) {
            throw new ContainerLaunchException("Timed out waiting for container port to open (" +
                    container.getContainerIpAddress() +
                    " ports: " +
                    externalLivenessCheckPorts +
                    " should be listening)");
        }
    }
}
