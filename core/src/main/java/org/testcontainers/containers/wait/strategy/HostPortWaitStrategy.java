package org.testcontainers.containers.wait.strategy;

import lombok.extern.slf4j.Slf4j;
import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.InternetProtocol;
import org.testcontainers.containers.Port;
import org.testcontainers.containers.wait.internal.ExternalPortListeningCheck;
import org.testcontainers.containers.wait.internal.InternalCommandPortListeningCheck;

import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

/**
 * Waits until a socket connection can be established on a port exposed or mapped by the container.
 *
 * @author richardnorth
 */
@Slf4j
public class HostPortWaitStrategy extends AbstractWaitStrategy {

    @Override
    protected void waitUntilReady() {
        final Set<Port> externalLivenessCheckPorts = getLivenessCheckPortsWithProtocols();

        if (externalLivenessCheckPorts.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug("Liveness check ports of {} is empty. Not waiting.", waitStrategyTarget.getContainerInfo().getName());
            }
            return;
        }

        @SuppressWarnings("unchecked")
        Set<Port> exposedPorts = waitStrategyTarget.exposedPorts();

        final Set<Port> internalPorts = getInternalPorts(externalLivenessCheckPorts, exposedPorts);

        Callable<Boolean> internalCheck = getListeningPortCheckCallable(internalPorts, this::toInternalPortListeningCheck);
        Callable<Boolean> externalCheck = getListeningPortCheckCallable(externalLivenessCheckPorts, this::toExternalPortListeningCheck);

        try {
            Unreliables.retryUntilTrue((int) startupTimeout.getSeconds(), TimeUnit.SECONDS,
                () -> getRateLimiter().getWhenReady(() -> internalCheck.call() && externalCheck.call()));

        } catch (TimeoutException e) {
            throw new ContainerLaunchException("Timed out waiting for container port to open (" +
                waitStrategyTarget.getHost() +
                " ports: " +
                externalLivenessCheckPorts +
                " should be listening)");
        }
    }

    private Callable<Boolean> getListeningPortCheckCallable(Set<Port> ports, BiFunction<InternetProtocol, Set<Integer>, Callable<Boolean>> function) {
        return ports.stream()
            .collect(Collectors.groupingBy(Port::getInternetProtocol, Collectors.mapping(Port::getValue, Collectors.toSet())))
            .entrySet()
            .stream()
            .map(entry -> function.apply(entry.getKey(), entry.getValue()))
            .reduce(() -> true, (portCheck1, portCheck2) -> () -> portCheck1.call() && portCheck2.call());
    }

    private Callable<Boolean> toExternalPortListeningCheck(InternetProtocol internetProtocol, Set<Integer> ports) {
        return new ExternalPortListeningCheck(waitStrategyTarget, ports, internetProtocol);
    }

    private Callable<Boolean> toInternalPortListeningCheck(InternetProtocol internetProtocol, Set<Integer> ports) {
        return new InternalCommandPortListeningCheck(waitStrategyTarget, ports, internetProtocol);
    }

    private Set<Port> getInternalPorts(Set<Port> externalLivenessCheckPorts, Set<Port> exposedPorts) {
        return exposedPorts.stream()
            .filter(port -> {
                Integer mappedPort = waitStrategyTarget.getMappedPort(port.getValue(), port.getInternetProtocol());
                return externalLivenessCheckPorts.contains(Port.of(mappedPort, port.getInternetProtocol()));
            })
            .collect(Collectors.toSet());
    }
}
