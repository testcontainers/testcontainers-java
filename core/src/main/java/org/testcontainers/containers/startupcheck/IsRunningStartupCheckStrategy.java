package org.testcontainers.containers.startupcheck;

import org.testcontainers.controller.ContainerController;
import org.testcontainers.controller.model.ContainerState;
import org.testcontainers.utility.DockerStatus;

/**
 * Simplest possible implementation of {@link StartupCheckStrategy} - just check that the container
 * has reached the running state and has not exited.
 */
public class IsRunningStartupCheckStrategy extends StartupCheckStrategy {

    @Override
    public StartupStatus checkStartupState(ContainerController containerController, String containerId) {
        ContainerState state = getCurrentState(containerController, containerId);
        if (state.getRunning()) {
            return StartupStatus.SUCCESSFUL;
        } else if (!DockerStatus.isContainerExitCodeSuccess(state)) {
            return StartupStatus.FAILED;
        } else {
            return StartupStatus.NOT_YET_KNOWN;
        }
    }
}
