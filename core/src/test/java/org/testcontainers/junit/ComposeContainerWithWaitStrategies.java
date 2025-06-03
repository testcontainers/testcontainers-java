package org.testcontainers.junit;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

public class ComposeContainerWithWaitStrategies {

    private static final int REDIS_PORT = 6379;

    @Test
    public void testComposeContainerConstructor() {
        try (
            // composeContainerWithCombinedWaitStrategies {
            ComposeContainer compose = new ComposeContainer(new File("src/test/resources/composev2/compose-test.yml"))
                .withExposedService("redis-1", REDIS_PORT, Wait.forSuccessfulCommand("redis-cli ping"))
                .withExposedService("db-1", 3306, Wait.forLogMessage(".*ready for connections.*\\n", 1))
            // }
        ) {
            compose.start();
            containsStartedServices(compose, "redis-1", "db-1");
        }
    }

    @Test
    public void testComposeContainerWaitForPortWithTimeout() {
        try (
            // composeContainerWaitForPortWithTimeout {
            ComposeContainer compose = new ComposeContainer(new File("src/test/resources/composev2/compose-test.yml"))
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

    private void containsStartedServices(ComposeContainer compose, String... expectedServices) {
        for (String serviceName : expectedServices) {
            assertThat(compose.getContainerByServiceName(serviceName))
                .as("Container should be found by service name %s", serviceName)
                .isPresent();
        }
    }
}
