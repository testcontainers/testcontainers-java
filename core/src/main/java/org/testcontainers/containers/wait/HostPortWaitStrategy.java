package org.testcontainers.containers.wait;

import lombok.extern.slf4j.Slf4j;
import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.dockerclient.ProxiedUnixSocketClientProviderStrategy;
import org.testcontainers.dockerclient.WindowsClientProviderStrategy;

import java.net.Socket;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;

/**
 * Waits until a socket connection can be established on a port exposed or mapped by the container.
 *
 * @author richardnorth
 */
@Slf4j
public class HostPortWaitStrategy extends GenericContainer.AbstractWaitStrategy {

    private static final String SUCCESS_MARKER = "TESTCONTAINERS_SUCCESS";
    @Override
    protected void waitUntilReady() {
        final Integer port = getLivenessCheckPort();
        if (null == port) {
            log.debug("Liveness check port of {} is empty. Not waiting.", container.getContainerName());
            return;
        }

        Callable<Boolean> checkStrategy;

        if (shouldCheckWithCommand()) {
            List<Integer> exposedPorts = container.getExposedPorts();

            Integer exposedPort = exposedPorts.stream()
                    .filter(it -> port.equals(container.getMappedPort(it)))
                    .findFirst()
                    .orElse(null);

            if (null == exposedPort) {
                log.warn("Liveness check port of {} is set to {}, but it's not listed in exposed ports.",
                        container.getContainerName(), port);
                return;
            }

            String[][] commands = {
                     { "/bin/sh", "-c", "nc -vz -w 1 localhost " + exposedPort + " && echo " + SUCCESS_MARKER },
                     { "/bin/bash", "-c", "</dev/tcp/localhost/" + exposedPort + " && echo " + SUCCESS_MARKER }
            };

            checkStrategy = () -> {
                for (String[] command : commands) {
                    try {
                        if (container.execInContainer(command).getStdout().contains(SUCCESS_MARKER)) {
                            return true;
                        }
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    } catch (Exception e) {
                        continue;
                    }
                }

                return false;
            };
        } else {
            checkStrategy = () -> {
                new Socket(container.getContainerIpAddress(), port).close();
                return true;
            };
        }

        try {
            Unreliables.retryUntilTrue((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> {
                return getRateLimiter().getWhenReady(() -> {
                    try {
                        return checkStrategy.call();
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
            });

        } catch (TimeoutException e) {
            throw new ContainerLaunchException("Timed out waiting for container port to open (" +
                    container.getContainerIpAddress() + ":" + port + " should be listening)");
        }
    }

    private boolean shouldCheckWithCommand() {
        // Special case for Docker for Mac, see #160
        if(DockerClientFactory.instance().isUsing(ProxiedUnixSocketClientProviderStrategy.class)
                && System.getProperty("os.name").toLowerCase().contains("mac")) {
            return true;
        }

        // Special case for Docker for Windows, see #160
        if (DockerClientFactory.instance().isUsing(WindowsClientProviderStrategy.class)) {
            return true;
        }

        return false;
    }
}
