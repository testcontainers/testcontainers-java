package org.testcontainers.containers.wait.strategy;

import org.testcontainers.containers.ContainerState;

import java.util.Set;
import java.util.stream.Collectors;

public interface WaitStrategyTarget extends ContainerState {

    /**
     * @return the ports on which to check if the container is ready
     */
    default Set<Integer> getLivenessCheckPortNumbers() {
        final Set<Integer> result = getExposedPorts().stream()
            .map(this::getMappedPort).distinct().collect(Collectors.toSet());
        result.addAll(getBoundPortNumbers());
        return result;
    }
}
