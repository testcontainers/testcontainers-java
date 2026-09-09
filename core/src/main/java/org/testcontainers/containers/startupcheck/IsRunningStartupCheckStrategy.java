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

    @Override
    @SuppressWarnings("deprecation")
    public boolean waitUntilStartupSuccessful(GenericContainer<?> container) {
        InspectContainerResponse.ContainerState cachedState = container.getContainerInfo().getState();
        StartupStatus cachedStatus = checkState(cachedState);

        if (cachedStatus == StartupStatus.SUCCESSFUL) {
            // Cached state shows the container as running/exited-success — verify with
            // one live Docker inspect to detect stale state (e.g., container crashed
            // between the port-mapping check and this startup check).
            try {
                if (
                    checkStartupState(container.getDockerClient(), container.getContainerId()) ==
                    StartupStatus.SUCCESSFUL
                ) {
                    return true;
                }
                // Live state doesn't match cached — container may have crashed.
                // Fall through to full rate-limited polling.
            } catch (Exception e) {
                // Live inspect failed (e.g., Docker timeout on slow CI) — trust
                // the cached state as the best available information.
                return true;
            }
        } else if (cachedStatus == StartupStatus.FAILED) {
            // Container already exited with a non-zero exit code
            return false;
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
