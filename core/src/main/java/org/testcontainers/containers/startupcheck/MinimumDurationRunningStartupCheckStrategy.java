package org.testcontainers.containers.startupcheck;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.utility.DockerStatus;

import java.time.Duration;
import java.time.Instant;

/**
 * Implementation of {@link StartupCheckStrategy} that checks the container is running and has been running for
 * a defined minimum period of time.
 */
public class MinimumDurationRunningStartupCheckStrategy extends StartupCheckStrategy {

    @NotNull
    private final Duration minimumRunningDuration;

    public MinimumDurationRunningStartupCheckStrategy(@NotNull Duration minimumRunningDuration) {
        this.minimumRunningDuration = minimumRunningDuration;
    }

    @Override
    public StartupStatus checkStartupState(DockerClient dockerClient, String containerId) {
        // record "now" before fetching status; otherwise the time to fetch the status
        // will contribute to how long the container has been running.
        Instant now = Instant.now();
        InspectContainerResponse.ContainerState state = getCurrentState(dockerClient, containerId);

        if (DockerStatus.isContainerRunning(state, minimumRunningDuration, now)) {
            return StartupStatus.SUCCESSFUL;
        } else if (DockerStatus.isContainerStopped(state)) {
            return StartupStatus.FAILED;
        }
        return StartupStatus.NOT_YET_KNOWN;
    }

}
