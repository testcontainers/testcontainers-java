package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.lifecycle.Startable;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that static {@link Container @Container} fields are available in non-static
 * {@link MethodSource @MethodSource} factory methods with
 * {@link TestInstance.Lifecycle#PER_CLASS PER_CLASS} lifecycle.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Testcontainers
class TestcontainersPerClassPostProcessorTest {

    @Container
    private static final StartTrackingMock staticContainer = new StartTrackingMock();

    @Container
    private final StartTrackingMock instanceContainer = new StartTrackingMock();

    private boolean staticStartedDuringMethodSource;

    private boolean instanceStartedDuringMethodSource;

    Stream<String> arguments() {
        staticStartedDuringMethodSource = staticContainer.containerId != null;
        instanceStartedDuringMethodSource = instanceContainer.containerId != null;
        return Stream.of("a");
    }

    @ParameterizedTest
    @MethodSource("arguments")
    void containers_are_started_before_method_source(String argument) {
        assertThat(staticStartedDuringMethodSource)
            .as("Static container should be started before @MethodSource resolution")
            .isTrue();
        assertThat(instanceStartedDuringMethodSource)
            .as("Instance container should NOT be started before @MethodSource resolution")
            .isFalse();
    }

    static class StartTrackingMock implements Startable {

        String containerId;

        @Override
        public void start() {
            containerId = UUID.randomUUID().toString();
        }

        @Override
        public void stop() {}
    }
}
