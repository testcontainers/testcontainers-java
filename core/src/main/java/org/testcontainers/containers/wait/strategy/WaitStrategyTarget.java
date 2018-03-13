package org.testcontainers.containers.wait.strategy;

import org.slf4j.Logger;
import org.testcontainers.containers.ContainerState;

import java.util.HashSet;
import java.util.Set;

public interface WaitStrategyTarget extends ContainerState {

    /**
     * @return the ports on which to check if the container is ready
     */
    default Set<Integer> getLivenessCheckPortNumbers() {
        final Set<Integer> result = new HashSet<>(getExposedPortNumbers());
        result.addAll(getBoundPortNumbers());
        return result;
    }

    Logger getLogger();
}
