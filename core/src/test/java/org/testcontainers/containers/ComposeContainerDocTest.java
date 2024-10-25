package org.testcontainers.containers;

import org.junit.Test;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class ComposeContainerDocTest {

    private static final int REDIS_PORT = 6379;
    private static final int POSTGRES_PORT = 5432;
    private static final String DOCKER_COMPOSE_FILE_PATH = "src/test/resources/v2-compose-test-doc.yml";
    public static final String ENV_FILE_NAME = "v2-compose-test-doc.env";

    @Test
    public void testComposeContainerConstructor() {
        try (
            // composeContainerConstructor {
            ComposeContainer compose = new ComposeContainer(new File(DOCKER_COMPOSE_FILE_PATH))
                .withExposedService("redis-1", REDIS_PORT)
                .withExposedService("postgres-1", POSTGRES_PORT)
            // }
        ) {
            compose.start();

            // getServiceHostAndPort {
            String redisUrl = String.format(
                "%s:%s",
                compose.getServiceHost("redis-1", REDIS_PORT),
                compose.getServicePort("redis-1", REDIS_PORT)
            );
            // }
            assertThat(redisUrl).isNotBlank();

            containsStartedServices(compose, "redis-1", "postgres-1");
        }
    }

    @Test
    public void testComposeContainerWithCombinedWaitStrategies() {
        try (
            // composeContainerWithCombinedWaitStrategies {
            ComposeContainer compose = new ComposeContainer(new File(DOCKER_COMPOSE_FILE_PATH))
                .withExposedService("redis-1", REDIS_PORT, Wait.forSuccessfulCommand("redis-cli ping"))
                .withExposedService(
                    "postgres-1",
                    POSTGRES_PORT,
                    Wait.forLogMessage(".*database system is ready to accept connections.*\\n", 1)
                )
            // }
        ) {
            compose.start();
            containsStartedServices(compose, "redis-1", "postgres-1");
        }
    }

    @Test
    public void testComposeContainerWaitForPortWithTimeout() {
        try (
            // composeContainerWaitForPortWithTimeout {
            ComposeContainer compose = new ComposeContainer(new File(DOCKER_COMPOSE_FILE_PATH))
                .withExposedService(
                    "redis-1",
                    REDIS_PORT,
                    Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(30))
                )
            // }
        ) {
            compose.start();
            containsStartedServices(compose, "redis-1");
        }
    }

    @Test
    public void testComposeContainerWithLocalCompose() {
        try (
            // composeContainerWithLocalCompose {
            ComposeContainer compose = new ComposeContainer(new File(DOCKER_COMPOSE_FILE_PATH))
                .withExposedService("redis-1", REDIS_PORT)
                .withLocalCompose(true)
            // }
        ) {
            compose.start();
            containsStartedServices(compose, "redis-1");
        }
    }

    @Test
    public void test() {
        try (
            // composeContainerWithCopyFiles {
            ComposeContainer compose = new ComposeContainer(new File(DOCKER_COMPOSE_FILE_PATH))
                .withExposedService("postgres-1", POSTGRES_PORT)
                .withCopyFilesInContainer(ENV_FILE_NAME)
            // }
        ) {
            compose.start();
            containsStartedServices(compose, "postgres-1");
        }
    }

    private void containsStartedServices(ComposeContainer compose, String... expectedServices) {
        final List<String> containerNames = compose
            .listChildContainers()
            .stream()
            .flatMap(it -> Stream.of(it.getNames()))
            .collect(Collectors.toList());

        for (String service : expectedServices) {
            assertThat(containerNames).anyMatch(it -> it.endsWith(service));
        }
    }
}
