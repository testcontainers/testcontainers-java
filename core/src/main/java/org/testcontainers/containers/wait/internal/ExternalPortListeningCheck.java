package org.testcontainers.containers.wait.internal;

import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.InternetProtocol;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

/**
 * Mechanism for testing that a socket is listening when run from the test host.
 */
public class ExternalPortListeningCheck implements Callable<Boolean> {
    private final ContainerState containerState;
    private final Set<Integer> externalLivenessCheckPorts;
    private final InternetProtocol internetProtocol;
    private final Map<InternetProtocol, BiConsumer<String, Integer>> listeningCheckHandlers;

    public ExternalPortListeningCheck(ContainerState containerState, Set<Integer> externalLivenessCheckPorts) {
        this(containerState, externalLivenessCheckPorts, InternetProtocol.TCP);
    }

    public ExternalPortListeningCheck(ContainerState containerState, Set<Integer> externalLivenessCheckPorts, InternetProtocol internetProtocol) {
        this.containerState = containerState;
        this.externalLivenessCheckPorts = externalLivenessCheckPorts;
        this.internetProtocol = internetProtocol;
        this. listeningCheckHandlers = createListeningCheckHandlersMap();
    }

    @Override
    public Boolean call() {
        String address = containerState.getHost();

        externalLivenessCheckPorts.parallelStream().forEach(externalPort -> {
            Optional.ofNullable(listeningCheckHandlers.get(internetProtocol))
                .orElseThrow(() -> new IllegalStateException("Internet protocol " + internetProtocol + " not supported"))
                .accept(address, externalPort);

        });
        return true;
    }

    private Map<InternetProtocol, BiConsumer<String, Integer>> createListeningCheckHandlersMap() {
        Map<InternetProtocol, BiConsumer<String, Integer>> listeningCheckHandlers = new EnumMap<>(InternetProtocol.class);

        listeningCheckHandlers.put(InternetProtocol.TCP, tcpListeningCheckHandler());
        listeningCheckHandlers.put(InternetProtocol.UDP, udpListeningCheckHandler());

        return Collections.unmodifiableMap(listeningCheckHandlers);
    }

    private BiConsumer<String, Integer> tcpListeningCheckHandler() {
        return (host, port) -> {
            try {
                new Socket(host, port).close();
            } catch (IOException e) {
                throw new IllegalStateException("Socket not listening yet: " + port);
            }
        };
    }

    private BiConsumer<String, Integer> udpListeningCheckHandler() {
        return (host, port) -> {
            try {
                new DatagramSocket(port, InetAddress.getByName(host)).close();
                throw new IllegalStateException("Nothing is listening on UDP " + port);
            } catch (SocketException e) {
                //exception was thrown so there is something listening on this port
            } catch (UnknownHostException e) {
                throw new IllegalArgumentException("Host not found", e);
            }
        };
    }
}
