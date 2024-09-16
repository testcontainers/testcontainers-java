package org.testcontainers.containers.wait.strategy;

import org.testcontainers.utility.ducttape.TimeoutException;
import org.testcontainers.utility.ducttape.Unreliables;
import org.testcontainers.containers.ContainerLaunchException;

import java.util.concurrent.TimeUnit;

public class ShellStrategy extends AbstractWaitStrategy {

    private String command;

    public ShellStrategy withCommand(String command) {
        this.command = command;
        return this;
    }

    @Override
    protected void waitUntilReady() {
        try {
            Unreliables.retryUntilTrue(
                (int) startupTimeout.getSeconds(),
                TimeUnit.SECONDS,
                () -> waitStrategyTarget.execInContainer("/bin/sh", "-c", this.command).getExitCode() == 0
            );
        } catch (TimeoutException e) {
            throw new ContainerLaunchException(
                "Timed out waiting for container to execute `" + this.command + "` successfully."
            );
        }
    }
}
