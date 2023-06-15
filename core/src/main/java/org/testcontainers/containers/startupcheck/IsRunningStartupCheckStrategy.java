package org.testcontainers.containers.startupcheck;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerStatus;

/**
 * Simplest possible implementation of {@link StartupCheckStrategy} - just check that the container
 * has reached the running state and has not exited.
 */
public class IsRunningStartupCheckStrategy extends StartupCheckStrategy {

    @SuppressWarnings("deprecation")
    @Override
    public boolean waitUntilStartupSuccessful(GenericContainer<?> container) {
        // Optimization: container already has the initial "after start" state, check it first
        if (checkState(container.getContainerInfo().getState()) == StartupStatus.SUCCESSFUL) {
            return true;
        }
        return super.waitUntilStartupSuccessful(container);
    }

    @Override
    public StartupStatus checkStartupState(DockerClient dockerClient, String containerId) {
        InspectContainerResponse.ContainerState state = getCurrentState(dockerClient, containerId);
        return checkState(state);
    }

    private StartupStatus checkState(InspectContainerResponse.ContainerState state) {
        if (Boolean.TRUE.equals(state.getRunning())) {
            return StartupStatus.SUCCESSFUL;
        } else if (DockerStatus.isContainerStopped(state)) {
            if (DockerStatus.isContainerExitCodeSuccess(state)) {
                return StartupStatus.SUCCESSFUL;
            } else {
                return StartupStatus.FAILED;
            }
        } else {
            return StartupStatus.NOT_YET_KNOWN;
        }
    }
}
