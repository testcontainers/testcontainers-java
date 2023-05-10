package org.testcontainers.containers.wait.strategy;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.rnorth.ducttape.TimeoutException;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.utility.DockerStatus;

import java.util.concurrent.TimeUnit;

/**
 * Wait strategy for OneShotStartupStrategy containers to wait until the container finishes successfully.
 *
 */
public class ContainerFinishedWaitStrategy extends AbstractWaitStrategy {

    protected boolean isContainerFinished() {
        InspectContainerResponse.ContainerState state = waitStrategyTarget.getCurrentContainerInfo().getState();
        return DockerStatus.isContainerStopped(state) && DockerStatus.isContainerExitCodeSuccess(state);
    }

    @Override
    protected void waitUntilReady() {
        try {
            Unreliables.retryUntilTrue((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, this::isContainerFinished);
        } catch (TimeoutException e) {
            throw new ContainerLaunchException("Timed out waiting for container to finish");
        }
    }
}
