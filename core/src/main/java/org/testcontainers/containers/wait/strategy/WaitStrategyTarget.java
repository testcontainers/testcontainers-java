package org.testcontainers.containers.wait.strategy;

import org.testcontainers.containers.ContainerState;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public interface WaitStrategyTarget extends ContainerState {
    /**
     * @return the ports on which to check if the container is ready
     */
    default Set<Integer> getLivenessCheckPortNumbers() {
        if (getContainerInfo().getHostConfig().getNetworkMode().equals("host")) {
            // On network mode "host", the container is directly connected to the host network stack.
            // Thus, there are no mapped ports or bound ports, and we should use the exposed ports.
            return new HashSet<>(getExposedPorts());
        } else {
            final Set<Integer> result = getExposedPorts()
                .stream()
                .map(this::getMappedPort)
                .distinct()
                .collect(Collectors.toSet());
            result.addAll(getBoundPortNumbers());
            return result;
        }
    }
}
