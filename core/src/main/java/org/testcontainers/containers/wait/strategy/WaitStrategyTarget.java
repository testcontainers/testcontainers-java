package org.testcontainers.containers.wait.strategy;

import org.testcontainers.containers.CommandExecutor;
import org.testcontainers.containers.LogFollower;

import java.util.HashSet;
import java.util.Set;

public interface WaitStrategyTarget extends CommandExecutor, LogFollower {

    /**
     * @return the ports on which to check if the container is ready
     */
    default Set<Integer> getLivenessCheckPortNumbers() {
        final Set<Integer> result = new HashSet<>(getExposedPortNumbers());
        result.addAll(getBoundPortNumbers());
        return result;
    }
}
