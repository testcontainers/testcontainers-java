package org.testcontainers.containers;

import org.junit.Test;
import org.rnorth.ducttape.TimeoutException;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class ComposeContainerWithServicesTest {

    public static final File SIMPLE_COMPOSE_FILE = new File(
        "src/test/resources/compose-scaling-multiple-containers.yml"
    );

    public static final File COMPOSE_FILE_WITH_INLINE_SCALE = new File(
        "src/test/resources/compose-with-inline-scale-test.yml"
    );

    @Test
    public void testDesiredSubsetOfServicesAreStarted() {
        try (ComposeContainer compose = new ComposeContainer(SIMPLE_COMPOSE_FILE).withServices("redis")) {
            compose.start();

            verifyStartedContainers(compose, "redis-1");
        }
    }

    @Test
    public void testDesiredSubsetOfScaledServicesAreStarted() {
        try (ComposeContainer compose = new ComposeContainer(SIMPLE_COMPOSE_FILE).withScaledService("redis", 2)) {
            compose.start();

            verifyStartedContainers(compose, "redis-1", "redis-2");
        }
    }

    @Test
    public void testDesiredSubsetOfSpecifiedAndScaledServicesAreStarted() {
        try (
            ComposeContainer compose = new ComposeContainer(SIMPLE_COMPOSE_FILE)
                .withServices("redis")
                .withScaledService("redis", 2)
        ) {
            compose.start();

            verifyStartedContainers(compose, "redis-1", "redis-2");
        }
    }

    @Test
    public void testDesiredSubsetOfSpecifiedOrScaledServicesAreStarted() {
        try (
            ComposeContainer compose = new ComposeContainer(SIMPLE_COMPOSE_FILE)
                .withServices("other")
                .withScaledService("redis", 2)
        ) {
            compose.start();

            verifyStartedContainers(compose, "redis-1", "redis-2", "other-1");
        }
    }

    @Test
    public void testAllServicesAreStartedIfNotSpecified() {
        try (ComposeContainer compose = new ComposeContainer(SIMPLE_COMPOSE_FILE)) {
            compose.start();

            verifyStartedContainers(compose, "redis-1", "other-1");
        }
    }

    @Test
    public void testScaleInComposeFileIsRespected() {
        try (ComposeContainer compose = new ComposeContainer(COMPOSE_FILE_WITH_INLINE_SCALE)) {
            compose.start();

            // the compose file includes `scale: 3` for the redis container
            verifyStartedContainers(compose, "redis-1", "redis-2", "redis-3");
        }
    }

    @Test
    public void testStartupTimeoutSetsTheHighestTimeout() {
        assertThat(
            catchThrowable(() -> {
                try (
                    ComposeContainer compose = new ComposeContainer(SIMPLE_COMPOSE_FILE)
                        .withServices("redis")
                        .withStartupTimeout(Duration.ofMillis(1))
                        .withExposedService(
                            "redis",
                            80,
                            Wait.forListeningPort().withStartupTimeout(Duration.ofMinutes(1))
                        );
                ) {
                    compose.start();
                }
            })
        )
            .as("We expect a timeout from the startup timeout")
            .isInstanceOf(TimeoutException.class);
    }

    private void verifyStartedContainers(final ComposeContainer compose, final String... names) {
        final List<String> containerNames = compose
            .listChildContainers()
            .stream()
            .flatMap(container -> Stream.of(container.getNames()))
            .collect(Collectors.toList());

        assertThat(containerNames)
            .as("number of running services of docker-compose is the same as length of listOfServices")
            .hasSize(names.length);

        for (final String expectedName : names) {
            final long matches = containerNames.stream().filter(foundName -> foundName.endsWith(expectedName)).count();

            assertThat(matches)
                .as("container with name starting '" + expectedName + "' should be running")
                .isEqualTo(1L);
        }
    }
}
