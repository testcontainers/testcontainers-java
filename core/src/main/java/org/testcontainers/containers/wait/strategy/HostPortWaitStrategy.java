package org.testcontainers.containers.wait.strategy;

import com.google.common.collect.ImmutableSet;
import lombok.extern.slf4j.Slf4j;
import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.wait.internal.ExternalPortListeningCheck;
import org.testcontainers.containers.wait.internal.InternalCommandPortListeningCheck;

import java.util.ArrayList;
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
public class HostPortWaitStrategy extends AbstractWaitStrategy {

    private List<Integer> expectedExposedPorts = new ArrayList<>();

    @Override
    protected void waitUntilReady() {
        final Set<Integer> externalLivenessCheckPorts = getLivenessCheckPorts();
        if (externalLivenessCheckPorts.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("Liveness check ports of {} is empty. Not waiting.", waitStrategyTarget.getContainerInfo().getName());
            }
            return;
        }

        List<Integer> exposedPorts;
        if (expectedExposedPorts.isEmpty()) {
            exposedPorts = waitStrategyTarget.getExposedPorts();
        } else {
            exposedPorts = expectedExposedPorts;
        }

        final Set<Integer> internalPorts = getInternalPorts(externalLivenessCheckPorts, exposedPorts);

        Callable<Boolean> internalCheck = new InternalCommandPortListeningCheck(waitStrategyTarget, internalPorts);

        Callable<Boolean> externalCheck = new ExternalPortListeningCheck(waitStrategyTarget, ImmutableSet.copyOf(exposedPorts));

        try {
            Unreliables.retryUntilTrue((int) startupTimeout.getSeconds(), TimeUnit.SECONDS,
                () -> getRateLimiter().getWhenReady(() -> internalCheck.call() && externalCheck.call()));

        } catch (TimeoutException e) {
            throw new ContainerLaunchException("Timed out waiting for container port to open (" +
                    waitStrategyTarget.getContainerIpAddress() +
                    " ports: " +
                    exposedPorts +
                    " should be listening)");
        }
    }

    public HostPortWaitStrategy withExpectedExposedPort(int exposedPort) {
        expectedExposedPorts.add(exposedPort);
        return this;
    }

    private Set<Integer> getInternalPorts(Set<Integer> externalLivenessCheckPorts, List<Integer> exposedPorts) {
        return exposedPorts.stream()
                .filter(it -> externalLivenessCheckPorts.contains(waitStrategyTarget.getMappedPort(it)))
                .collect(Collectors.toSet());
    }
}
