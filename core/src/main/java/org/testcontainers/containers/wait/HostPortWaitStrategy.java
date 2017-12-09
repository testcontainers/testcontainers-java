package org.testcontainers.containers.wait;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.SystemUtils;
import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.internal.ExternalPortListeningCheck;
import org.testcontainers.containers.wait.internal.InternalCommandPortListeningCheck;
import org.testcontainers.dockerclient.DockerMachineClientProviderStrategy;
import org.testcontainers.dockerclient.WindowsClientProviderStrategy;

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

        Callable<Boolean> check;

        if (shouldCheckWithCommand()) {
            List<Integer> exposedPorts = container.getExposedPorts();

            final Set<Integer> internalPorts = exposedPorts.stream()
                    .filter(it -> externalLivenessCheckPorts.contains(container.getMappedPort(it)))
                    .collect(Collectors.toSet());

            check = new InternalCommandPortListeningCheck(container, internalPorts);
        } else {
            check = new ExternalPortListeningCheck(container.getContainerIpAddress(), externalLivenessCheckPorts);
        }

        try {
            Unreliables.retryUntilTrue((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> {
                return getRateLimiter().getWhenReady(check);
            });

        } catch (TimeoutException e) {
            throw new ContainerLaunchException("Timed out waiting for container port to open (" +
                    container.getContainerIpAddress() +
                    " ports: " +
                    externalLivenessCheckPorts +
                    " should be listening)");
        }
    }

    private boolean shouldCheckWithCommand() {
        // Special case for Docker for Mac, see #160
        if (!DockerClientFactory.instance().isUsing(DockerMachineClientProviderStrategy.class) &&
                SystemUtils.IS_OS_MAC_OSX) {
            return true;
        }

        // Special case for Docker for Windows, see #160
        if (DockerClientFactory.instance().isUsing(WindowsClientProviderStrategy.class)) {
            return true;
        }

        return false;
    }

}
