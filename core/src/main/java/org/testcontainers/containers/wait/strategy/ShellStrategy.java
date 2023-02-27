package org.testcontainers.containers.wait.strategy;

import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.ContainerLaunchException;

import java.util.concurrent.TimeUnit;

public class ShellStrategy extends AbstractWaitStrategy {

    private final String command;

    public ShellStrategy(String command) {
        this.command = command;
    }

    @Override
    protected void waitUntilReady() {
        try {
            Unreliables.retryUntilTrue(
                (int) startupTimeout.getSeconds(),
                TimeUnit.SECONDS,
                () -> waitStrategyTarget.execInContainer("/bin/sh", "-c", command).getExitCode() == 0
            );
        } catch (TimeoutException e) {
            throw new ContainerLaunchException("Timed out waiting for container to execute command successfully");
        }
    }
}
