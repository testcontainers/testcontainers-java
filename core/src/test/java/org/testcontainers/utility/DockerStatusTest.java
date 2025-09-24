package org.testcontainers.utility;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ParameterizedClass
@MethodSource("parameters")
class DockerStatusTest {

    private DateTimeFormatter dateTimeFormatter;

    private static final Instant now = Instant.now();

    private final InspectContainerResponse.ContainerState running;

    private final InspectContainerResponse.ContainerState runningVariant;

    private final InspectContainerResponse.ContainerState shortRunning;

    private final InspectContainerResponse.ContainerState created;

    // a container in the "created" state is not running, and has both startedAt and finishedAt empty.
    private final InspectContainerResponse.ContainerState createdVariant;

    private final InspectContainerResponse.ContainerState exited;

    private final InspectContainerResponse.ContainerState paused;

    private static final Duration minimumDuration = Duration.ofMillis(20);

    public DockerStatusTest(DateTimeFormatter dateTimeFormatter) {
        this.dateTimeFormatter = dateTimeFormatter;
        running = buildState(true, false, buildTimestamp(now.minusMillis(30)), DockerStatus.DOCKER_TIMESTAMP_ZERO);
        runningVariant = buildState(true, false, buildTimestamp(now.minusMillis(30)), "");
        shortRunning = buildState(true, false, buildTimestamp(now.minusMillis(10)), DockerStatus.DOCKER_TIMESTAMP_ZERO);
        created = buildState(false, false, DockerStatus.DOCKER_TIMESTAMP_ZERO, DockerStatus.DOCKER_TIMESTAMP_ZERO);
        createdVariant = buildState(false, false, null, null);
        exited = buildState(false, false, buildTimestamp(now.minusMillis(100)), buildTimestamp(now.minusMillis(90)));
        paused = buildState(false, true, buildTimestamp(now.minusMillis(100)), DockerStatus.DOCKER_TIMESTAMP_ZERO);
    }

    public static Stream<DateTimeFormatter> parameters() {
        return Stream.of(
            DateTimeFormatter.ISO_INSTANT,
            DateTimeFormatter.ISO_OFFSET_DATE_TIME.withZone(ZoneId.of("America/New_York"))
        );
    }

    @Test
    void testRunning() {
        assertThat(DockerStatus.isContainerRunning(running, minimumDuration, now)).isTrue();
        assertThat(DockerStatus.isContainerRunning(runningVariant, minimumDuration, now)).isTrue();
        assertThat(DockerStatus.isContainerRunning(shortRunning, minimumDuration, now)).isFalse();
        assertThat(DockerStatus.isContainerRunning(created, minimumDuration, now)).isFalse();
        assertThat(DockerStatus.isContainerRunning(createdVariant, minimumDuration, now)).isFalse();
        assertThat(DockerStatus.isContainerRunning(exited, minimumDuration, now)).isFalse();
        assertThat(DockerStatus.isContainerRunning(paused, minimumDuration, now)).isFalse();
    }

    @Test
    void testStopped() {
        assertThat(DockerStatus.isContainerStopped(running)).isFalse();
        assertThat(DockerStatus.isContainerStopped(runningVariant)).isFalse();
        assertThat(DockerStatus.isContainerStopped(shortRunning)).isFalse();
        assertThat(DockerStatus.isContainerStopped(created)).isFalse();
        assertThat(DockerStatus.isContainerStopped(createdVariant)).isFalse();
        assertThat(DockerStatus.isContainerStopped(exited)).isTrue();
        assertThat(DockerStatus.isContainerStopped(paused)).isFalse();
    }

    private String buildTimestamp(Instant instant) {
        return dateTimeFormatter.format(instant);
    }

    // ContainerState is a non-static inner class, with private member variables, in a different package.
    // It's simpler to mock it that to try to create one.
    private static InspectContainerResponse.ContainerState buildState(
        boolean running,
        boolean paused,
        String startedAt,
        String finishedAt
    ) {
        InspectContainerResponse.ContainerState state = Mockito.mock(InspectContainerResponse.ContainerState.class);
        when(state.getRunning()).thenReturn(running);
        when(state.getPaused()).thenReturn(paused);
        when(state.getStartedAt()).thenReturn(startedAt);
        when(state.getFinishedAt()).thenReturn(finishedAt);
        return state;
    }
}
