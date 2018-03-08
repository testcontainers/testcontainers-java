package org.testcontainers.containers.wait.strategy;

import org.testcontainers.ContainerState;

import java.time.Duration;

public interface WaitStrategy {

    void waitUntilReady(ContainerState containerState);

    WaitStrategy withStartupTimeout(Duration startupTimeout);
}
