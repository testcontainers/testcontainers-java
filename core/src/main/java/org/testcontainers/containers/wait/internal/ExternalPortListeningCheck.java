package org.testcontainers.containers.wait.internal;

import lombok.RequiredArgsConstructor;
import org.testcontainers.containers.ContainerState;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Set;
import java.util.concurrent.Callable;

/**
 * Mechanism for testing that a socket is listening when run from the test host.
 */
@RequiredArgsConstructor
public class ExternalPortListeningCheck implements Callable<Boolean> {
    private final ContainerState containerState;
    private final Set<Integer> externalLivenessCheckPorts;

    @Override
    public Boolean call() {
        String address = containerState.getHost();

        externalLivenessCheckPorts.parallelStream().forEach(externalPort -> {
            try (Socket socket = new Socket()) {
                InetSocketAddress inetSocketAddress = new InetSocketAddress(address, externalPort);
                socket.connect(inetSocketAddress, 1000);
            } catch (IOException e) {
                throw new IllegalStateException("Socket not listening yet: " + externalPort);
            }
        });
        return true;
    }
}
