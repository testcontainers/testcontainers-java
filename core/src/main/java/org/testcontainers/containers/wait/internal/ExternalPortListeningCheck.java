package org.testcontainers.containers.wait.internal;

import lombok.RequiredArgsConstructor;
import org.testcontainers.containers.ContainerState;

import java.io.IOException;
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
        String address = containerState.getContainerIpAddress();

        for (Integer externalPort : externalLivenessCheckPorts) {
            try {
                new Socket(address, externalPort).close();
            } catch (IOException e) {
                throw new IllegalStateException("Socket not listening yet: " + externalPort);
            }
        }
        return true;
    }
}
