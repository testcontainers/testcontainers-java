package org.testcontainers.containers.wait;

import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

/**
 * Waits until a socket connection can be established on a port exposed or mapped by the container.
 *
 * @author richardnorth
 */
public class HostPortWaitStrategy extends GenericContainer.AbstractWaitStrategy {
    @Override
    protected void waitUntilReady() {
        final Integer port = getLivenessCheckPort();
        if (null == port) {
            return;
        }

        final String ipAddress = DockerClientFactory.instance().dockerHostIpAddress();
        try {
            Unreliables.retryUntilSuccess((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> {
                getRateLimiter().doWhenReady(() -> {
                    try {
                        new Socket(ipAddress, port).close();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                return true;
            });

        } catch (TimeoutException e) {
            throw new ContainerLaunchException("Timed out waiting for container port to open (" +
                    ipAddress + ":" + port + " should be listening)");
        }
    }
}
