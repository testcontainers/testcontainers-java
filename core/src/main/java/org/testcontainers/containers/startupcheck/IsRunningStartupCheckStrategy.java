package org.testcontainers.containers.startupcheck;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.utility.DockerStatus;

/**
 * Simplest possible implementation of {@link StartupCheckStrategy} - just check that the container
 * has reached the running state and has not exited.
 */
public class IsRunningStartupCheckStrategy extends StartupCheckStrategy {

    @Override
    public StartupStatus checkStartupState(DockerClient dockerClient, String containerId) {
        InspectContainerResponse.ContainerState state = getCurrentState(dockerClient, containerId);
        if (state.getRunning()) {
            return StartupStatus.SUCCESSFUL;
        } else if (!DockerStatus.isContainerExitCodeSuccess(state)) {
            return StartupStatus.FAILED;
        } else {
            return StartupStatus.NOT_YET_KNOWN;
        }
    }
}
