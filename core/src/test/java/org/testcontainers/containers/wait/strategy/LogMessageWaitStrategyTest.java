package org.testcontainers.containers.wait.strategy;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class LogMessageWaitStrategyTest {

    @Test
    void shouldFailFastWhenContainerExitsBeforeExpectedLogOutput() {
        GenericContainer<?> container = new GenericContainer<>("alpine:3.20")
            .withCommand("sh", "-c", "echo started; sleep 2; exit 1")
            .waitingFor(Wait.forLogMessage(".*never.*", 1).withStartupTimeout(Duration.ofSeconds(60)));

        Instant start = Instant.now();
        Throwable thrown = catchThrowable(container::start);
        Duration elapsed = Duration.between(start, Instant.now());

        assertThat(thrown)
            .as("Container launch fails when the container exits before producing the expected log output")
            .isInstanceOf(ContainerLaunchException.class)
            .hasRootCauseMessage("Container stopped before the expected log output matching '.*never.*' was produced");
        assertThat(elapsed)
            .as("Fails fast instead of waiting for the whole startup timeout")
            .isLessThan(Duration.ofSeconds(20));
    }

    @Test
    void shouldStillTimeOutWhenContainerKeepsRunningWithoutExpectedLogOutput() {
        GenericContainer<?> container = new GenericContainer<>("alpine:3.20")
            .withCommand("sh", "-c", "sleep 30")
            .waitingFor(Wait.forLogMessage(".*never.*", 1).withStartupTimeout(Duration.ofSeconds(3)));

        Throwable thrown = catchThrowable(container::start);

        assertThat(thrown)
            .as("Timeout semantics for a still running container are unchanged")
            .isInstanceOf(ContainerLaunchException.class)
            .hasRootCauseMessage("Timed out waiting for log output matching '.*never.*'");
    }

    @Test
    void shouldSucceedWhenExpectedLogOutputAppearsWhileContainerIsRunning() {
        GenericContainer<?> container = new GenericContainer<>("alpine:3.20")
            .withCommand("sh", "-c", "echo ready; sleep 5")
            .waitingFor(Wait.forLogMessage(".*ready.*", 1).withStartupTimeout(Duration.ofSeconds(30)));

        container.start();

        assertThat(container.isRunning()).isTrue();
    }
}
