package org.testcontainers.utility;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.junit.Test;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 *
 */
public class DockerStatusTest {

    private static Instant now = Instant.now();

    private static InspectContainerResponse.ContainerState running =
        buildState(true, false, buildTimestamp(now.minusMillis(30)), DockerStatus.DOCKER_TIMESTAMP_ZERO);

    private static InspectContainerResponse.ContainerState runningVariant =
        buildState(true, false, buildTimestamp(now.minusMillis(30)), "");

    private static InspectContainerResponse.ContainerState shortRunning =
        buildState(true, false, buildTimestamp(now.minusMillis(10)), DockerStatus.DOCKER_TIMESTAMP_ZERO);

    private static InspectContainerResponse.ContainerState created =
        buildState(false, false, DockerStatus.DOCKER_TIMESTAMP_ZERO, DockerStatus.DOCKER_TIMESTAMP_ZERO);

    // a container in the "created" state is not running, and has both startedAt and finishedAt empty.
    private static InspectContainerResponse.ContainerState createdVariant =
        buildState(false, false, null, null);

    private static InspectContainerResponse.ContainerState exited =
        buildState(false, false, buildTimestamp(now.minusMillis(100)), buildTimestamp(now.minusMillis(90)));

    private static InspectContainerResponse.ContainerState paused =
        buildState(false, true, buildTimestamp(now.minusMillis(100)), DockerStatus.DOCKER_TIMESTAMP_ZERO);

    private static Duration minimumDuration = Duration.ofMillis(20);

    @Test
    public void testRunning() throws Exception {
        assertTrue(DockerStatus.isContainerRunning(running, minimumDuration, now));
        assertTrue(DockerStatus.isContainerRunning(runningVariant, minimumDuration, now));
        assertFalse(DockerStatus.isContainerRunning(shortRunning, minimumDuration, now));
        assertFalse(DockerStatus.isContainerRunning(created, minimumDuration, now));
        assertFalse(DockerStatus.isContainerRunning(createdVariant, minimumDuration, now));
        assertFalse(DockerStatus.isContainerRunning(exited, minimumDuration, now));
        assertFalse(DockerStatus.isContainerRunning(paused, minimumDuration, now));
    }

    @Test
    public void testStopped() throws Exception {
        assertFalse(DockerStatus.isContainerStopped(running));
        assertFalse(DockerStatus.isContainerStopped(runningVariant));
        assertFalse(DockerStatus.isContainerStopped(shortRunning));
        assertFalse(DockerStatus.isContainerStopped(created));
        assertFalse(DockerStatus.isContainerStopped(createdVariant));
        assertTrue(DockerStatus.isContainerStopped(exited));
        assertFalse(DockerStatus.isContainerStopped(paused));
    }

    @Test
    public void testTimestampsWithZoneOffset() {
        assertTrue(DockerStatus.isContainerStopped(buildState(false, false,
            "2018-09-18T08:56:35.1556366Z",
            "2018-09-18T10:56:35.7102779+02:00")
        ));
    }

    private static String buildTimestamp(Instant instant) {
        return DateTimeFormatter.ISO_INSTANT.format(instant);
    }

    // ContainerState is a non-static inner class, with private member variables, in a different package.
    // It's simpler to mock it that to try to create one.
    private static InspectContainerResponse.ContainerState buildState(boolean running, boolean paused,
                                                                      String startedAt, String finishedAt) {

        InspectContainerResponse.ContainerState state = Mockito.mock(InspectContainerResponse.ContainerState.class);
        when(state.getRunning()).thenReturn(running);
        when(state.getPaused()).thenReturn(paused);
        when(state.getStartedAt()).thenReturn(startedAt);
        when(state.getFinishedAt()).thenReturn(finishedAt);
        return state;
    }
}
