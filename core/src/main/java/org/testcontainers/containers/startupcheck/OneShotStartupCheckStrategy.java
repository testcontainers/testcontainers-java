package org.testcontainers.containers.startupcheck;

import org.testcontainers.controller.ContainerController;
import org.testcontainers.controller.model.ContainerState;
import org.testcontainers.utility.DockerStatus;

/**
 * Implementation of {@link StartupCheckStrategy} intended for use with containers that only run briefly and
 * exit of their own accord. As such, success is deemed to be when the container has stopped with exit code 0.
 */
public class OneShotStartupCheckStrategy extends StartupCheckStrategy {

    @Override
    public StartupStatus checkStartupState(ContainerController containerController, String containerId) {
        ContainerState state = getCurrentState(containerController, containerId);

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
