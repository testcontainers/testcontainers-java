package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.util.List;

public interface BaseConfiguration {
    List<Integer> getExposedPorts();

    WaitStrategy getWaitStrategy();
}
