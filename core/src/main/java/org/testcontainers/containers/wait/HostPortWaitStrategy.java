package org.testcontainers.containers.wait;

import org.testcontainers.containers.GenericContainer;

import java.net.Socket;
import java.util.Optional;

/**
 * Waits until a socket connection can be established on a port exposed or mapped by the container.
 *
 * @author richardnorth
 */
public class HostPortWaitStrategy extends GenericWaitStrategy<HostPortWaitStrategy> {

    private int port = 0;

    public HostPortWaitStrategy() {
        super("create socket");
    }

    /**
     * Set request port.
     *
     * @param port the port to check on
     * @return this
     */
    public HostPortWaitStrategy withPort(int port) {
        this.port = port;
        return this;
    }

    @Override
    protected boolean isReady(GenericContainer container) throws Exception {
        final String ipAddress = container.getContainerIpAddress();

        int readyPort;

        if (this.port != 0) {
            readyPort = container.getMappedPort(this.port);
        } else {
            Optional<Integer> primaryMappedContainerPort = getPrimaryMappedContainerPort(container);
            if (primaryMappedContainerPort.isPresent()) {
                readyPort = primaryMappedContainerPort.get();
            } else {
                return true;
            }
        }

        container.logger().info("create socket " + ipAddress + ":" + readyPort);
        new Socket(ipAddress, readyPort).close();
        return true;
    }
}
