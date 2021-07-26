package org.testcontainers.containers;

import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;


public class DockerComposeContainerWithServicesTest {

    public static final File SIMPLE_COMPOSE_FILE = new File("src/test/resources/compose-scaling-multiple-containers.yml");
    public static final File COMPOSE_FILE_WITH_INLINE_SCALE = new File("src/test/resources/compose-with-inline-scale-test.yml");

    @Test
    public void testDesiredSubsetOfServicesAreStarted() {
        try (
            DockerComposeContainer<?> compose = new DockerComposeContainer<>(SIMPLE_COMPOSE_FILE)
                .withServices("redis")
        ) {
            compose.start();

            verifyStartedContainers(compose, "redis_1");
        }
    }

    @Test
    public void testDesiredSubsetOfScaledServicesAreStarted() {
        try (
            DockerComposeContainer<?> compose = new DockerComposeContainer<>(SIMPLE_COMPOSE_FILE)
                .withScaledService("redis", 2)
        ) {
            compose.start();

            verifyStartedContainers(compose, "redis_1", "redis_2");
        }
    }

    @Test
    public void testDesiredSubsetOfSpecifiedAndScaledServicesAreStarted() {
        try (
            DockerComposeContainer<?> compose = new DockerComposeContainer<>(SIMPLE_COMPOSE_FILE)
                .withServices("redis")
                .withScaledService("redis", 2)
        ) {
            compose.start();

            verifyStartedContainers(compose, "redis_1", "redis_2");
        }
    }

    @Test
    public void testDesiredSubsetOfSpecifiedOrScaledServicesAreStarted() {
        try (
            DockerComposeContainer<?> compose = new DockerComposeContainer<>(SIMPLE_COMPOSE_FILE)
                .withServices("other")
                .withScaledService("redis", 2)
        ) {
            compose.start();

            verifyStartedContainers(compose, "redis_1", "redis_2", "other_1");
        }
    }

    @Test
    public void testAllServicesAreStartedIfNotSpecified() {
        try (
            DockerComposeContainer<?> compose = new DockerComposeContainer<>(SIMPLE_COMPOSE_FILE)
        ) {
            compose.start();

            verifyStartedContainers(compose, "redis_1", "other_1");
        }
    }

    @Test
    public void testScaleInComposeFileIsRespected() {
        try (
            DockerComposeContainer<?> compose = new DockerComposeContainer<>(COMPOSE_FILE_WITH_INLINE_SCALE)
        ) {
            compose.start();

            // the compose file includes `scale: 3` for the redis container
            verifyStartedContainers(compose, "redis_1", "redis_2", "redis_3");
        }
    }

    private void verifyStartedContainers(final DockerComposeContainer<?> compose, final String... names) {
        final List<String> containerNames = compose.listChildContainers().stream()
            .flatMap(container -> Stream.of(container.getNames()))
            .collect(Collectors.toList());

        assertEquals("number of running services of docker-compose is the same as length of listOfServices",
            names.length, containerNames.size());

        for (final String expectedName : names) {
            final long matches = containerNames.stream()
                .filter(foundName -> foundName.endsWith(expectedName))
                .count();

            assertEquals("container with name starting '" + expectedName + "' should be running", 1L, matches);
        }
    }
}
