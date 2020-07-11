package org.testcontainers.containers.wait.strategy;

import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.Port;

import java.util.Set;
import java.util.stream.Collectors;

public interface WaitStrategyTarget extends ContainerState {

    /**
     * @return the ports on which to check if the container is ready
     */
    default Set<Integer> getLivenessCheckPortNumbers() {
        final Set<Integer> result = getExposedPorts().stream()
            .map(this::getMappedPort)
            .collect(Collectors.toSet());
        result.addAll(getBoundPortNumbers());
        return result;
    }

    default Set<Port> getLivenessCheckPortsWithProtocols() {
        final Set<Port> result = exposedPorts()
            .stream()
            .map(port -> {
                Integer mappedPort = this.getMappedPort(port.getValue(), port.getInternetProtocol());
                return Port.of(mappedPort, port.getInternetProtocol());
            })
            .collect(Collectors.toSet());
        result.addAll(getBoundPorts());
        return result;
    }
}
