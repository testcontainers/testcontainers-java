package org.testcontainers.containers.startupcheck;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.utility.DockerStatus;

/**
 * Implementation of {@link StartupCheckStrategy} intended for use with containers that only run briefly and
 * exit of their own accord. As such, success is deemed to be when the container has stopped with exit code 0.
 */
public class OneShotStartupCheckStrategy extends StartupCheckStrategy {

    @Override
    public StartupStatus checkStartupState(DockerClient dockerClient, String containerId) {
        InspectContainerResponse.ContainerState state = getCurrentState(dockerClient, containerId);

        if (!DockerStatus.isContainerStopped(state)) {
            return StartupStatus.NOT_YET_KNOWN;
        }

        if (DockerStatus.isContainerStopped(state) && DockerStatus.isContainerExitCodeSuccess(state)) {
            return StartupStatus.SUCCESSFUL;
        } else {
            return StartupStatus.FAILED;
        }
    }
}
