package org.testcontainers.utility;

import com.github.dockerjava.api.command.InspectContainerResponse;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Utility functions for dealing with docker status based on the information available to us, and trying to be
 * defensive.
 * <p>
 * <p>In docker-java version 2.2.0, which we're using, only these
 * fields are available in the container state returned from Docker Inspect: "isRunning", "isPaused", "startedAt", and
 * "finishedAt". There are states that can occur (including "created", "OOMkilled" and "dead") that aren't directly
 * shown through this result.
 * <p>
 * <p>Docker also doesn't seem to use null values for timestamps; see DOCKER_TIMESTAMP_ZERO, below.
 */
public class DockerStatus {

    /**
     * When the docker client has an "empty" timestamp, it returns this special value, rather than
     * null or an empty string.
     */
    static final String DOCKER_TIMESTAMP_ZERO = "0001-01-01T00:00:00Z";

    /**
     * Based on this status, is this container running, and has it been doing so for the specified amount of time?
     *
     * @param state                  the state provided by InspectContainer
     * @param minimumRunningDuration minimum duration to consider this as "solidly" running, or null
     * @param now                    the time to consider as the current time
     * @return true if we can conclude that the container is running, false otherwise
     */
    public static boolean isContainerRunning(InspectContainerResponse.ContainerState state,
                                             Duration minimumRunningDuration,
                                             Instant now) {
        if (state.getRunning()) {
            if (minimumRunningDuration == null) {
                return true;
            }
            Instant startedAt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(
                state.getStartedAt(), Instant::from);

            if (startedAt.isBefore(now.minus(minimumRunningDuration))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Based on this status, has the container halted?
     *
     * @param state the state provided by InspectContainer
     * @return true if we can conclude that the container has started but is now stopped, false otherwise.
     */
    public static boolean isContainerStopped(InspectContainerResponse.ContainerState state) {

        // get some preconditions out of the way
        if (state.getRunning() || state.getPaused()) {
            return false;
        }

        // if the finished timestamp is non-empty, that means the container started and finished.
        boolean hasStarted = isDockerTimestampNonEmpty(state.getStartedAt());
        boolean hasFinished = isDockerTimestampNonEmpty(state.getFinishedAt());
        return hasStarted && hasFinished;
    }

    public static boolean isDockerTimestampNonEmpty(String dockerTimestamp) {
        // This is a defensive approach. Current versions of Docker use the DOCKER_TIMESTAMP_ZERO value, but
        // that could change.
        return dockerTimestamp != null
                && !dockerTimestamp.isEmpty()
                && !dockerTimestamp.equals(DOCKER_TIMESTAMP_ZERO)
                && DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(dockerTimestamp, Instant::from).getEpochSecond() >= 0L;
    }

    public static boolean isContainerExitCodeSuccess(InspectContainerResponse.ContainerState state) {
        int exitCode = state.getExitCode();
        // 0 is the only exit code we can consider as success
        return exitCode == 0;
    }
}
