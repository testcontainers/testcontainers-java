package org.testcontainers.junit;

import org.junit.Test;
import org.junit.runner.Description;
import org.rnorth.ducttape.TimeoutException;
import org.rnorth.visibleassertions.VisibleAssertions;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.Wait;

import java.io.File;
import java.time.Duration;

public class DockerComposeWaitStrategyTest {

    private static final int REDIS_PORT = 6379;

    @Test
    public void testWaitOnListeningPort() {
        final DockerComposeContainer environment = new DockerComposeContainer(new File("src/test/resources/compose-test.yml"))
            .withExposedService("redis_1", REDIS_PORT, Wait.forListeningPort());

        try {
            environment.starting(Description.createTestDescription(Object.class, "name"));
            VisibleAssertions.pass("Docker compose should start after waiting for listening port");
        } catch (TimeoutException e) {
            VisibleAssertions.fail("Docker compose should start after waiting for listening port with failed with: " + e);
        }
    }

    @Test
    public void testWaitOnMultipleStrategiesPassing() {
        final DockerComposeContainer environment = new DockerComposeContainer(new File("src/test/resources/compose-test.yml"))
            .withExposedService("redis_1", REDIS_PORT, Wait.forListeningPort())
            .withExposedService("db_1", 3306, Wait.forLogMessage(".*ready for connections.*\\s", 1))
            .withTailChildContainers(true);

        try {
            environment.starting(Description.createTestDescription(Object.class, "name"));
            VisibleAssertions.pass("Docker compose should start after waiting for listening port");
        } catch (TimeoutException e) {
            VisibleAssertions.fail("Docker compose should start after waiting for listening port with failed with: " + e);
        }
    }

    @Test
    public void testWaitingFails() {
        final DockerComposeContainer environment = new DockerComposeContainer(new File("src/test/resources/compose-test.yml"))
            .withStartupTimeout(Duration.ofSeconds(10))
            .withExposedService("redis_1", REDIS_PORT, Wait.forHttp("/test"));
        VisibleAssertions.assertThrows("waiting on an invalid http path times out",
            TimeoutException.class,
            () -> environment.starting(Description.createTestDescription(Object.class, "name")));
    }

    @Test
    public void testWaitOnOneOfMultipleStrategiesFailing() {
        final DockerComposeContainer environment = new DockerComposeContainer(new File("src/test/resources/compose-test.yml"))
            .withStartupTimeout(Duration.ofSeconds(10))
            .withExposedService("redis_1", REDIS_PORT, Wait.forListeningPort())
            .withExposedService("db_1", 3306, Wait.forLogMessage(".*test test test.*\\s", 1))
            .withTailChildContainers(true);

        VisibleAssertions.assertThrows("waiting on one failing strategy to time out",
            TimeoutException.class,
            () -> environment.starting(Description.createTestDescription(Object.class, "name")));
    }

}
