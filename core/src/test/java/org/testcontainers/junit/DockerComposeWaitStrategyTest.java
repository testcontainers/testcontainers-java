package org.testcontainers.junit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.io.File;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.fail;

public class DockerComposeWaitStrategyTest {

    private static final int REDIS_PORT = 6379;

    private DockerComposeContainer<?> environment;

    @Before
    public final void setUp() {
        environment = new DockerComposeContainer<>(new File("src/test/resources/compose-test.yml"));
    }

    @After
    public final void cleanUp() {
        environment.stop();
    }

    @Test
    public void testWaitOnListeningPort() {
        environment.withExposedService("redis_1", REDIS_PORT, Wait.forListeningPort());

        try {
            environment.start();
        } catch (RuntimeException e) {
            fail("Docker compose should start after waiting for listening port with failed with: " + e);
        }
    }

    @Test
    public void testWaitOnMultipleStrategiesPassing() {
        environment
            .withExposedService("redis_1", REDIS_PORT, Wait.forListeningPort())
            .withExposedService("db_1", 3306, Wait.forLogMessage(".*ready for connections.*\\s", 1))
            .withTailChildContainers(true);

        try {
            environment.start();
        } catch (RuntimeException e) {
            fail("Docker compose should start after waiting for listening port with failed with: " + e);
        }
    }

    @Test
    public void testWaitingFails() {
        environment.withExposedService(
            "redis_1",
            REDIS_PORT,
            Wait.forHttp("/test").withStartupTimeout(Duration.ofSeconds(10))
        );
        assertThat(catchThrowable(() -> environment.start()))
            .as("waiting on an invalid http path times out")
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void testWaitOnOneOfMultipleStrategiesFailing() {
        environment
            .withExposedService(
                "redis_1",
                REDIS_PORT,
                Wait.forListeningPort().withStartupTimeout(Duration.ofSeconds(10))
            )
            .waitingFor(
                "db_1",
                Wait.forLogMessage(".*test test test.*\\s", 1).withStartupTimeout(Duration.ofSeconds(10))
            )
            .withTailChildContainers(true);

        assertThat(catchThrowable(() -> environment.start()))
            .as("waiting on one failing strategy to time out")
            .isInstanceOf(RuntimeException.class);
    }

    @Test
    public void testWaitingForNonexistentServices() {
        String existentServiceName = "db_1";
        String nonexistentServiceName1 = "some_nonexistent_service_1";
        String nonexistentServiceName2 = "some_nonexistent_service_2";
        WaitStrategy someWaitStrategy = Mockito.mock(WaitStrategy.class);

        environment
            .waitingFor(existentServiceName, someWaitStrategy)
            .waitingFor(nonexistentServiceName1, someWaitStrategy)
            .waitingFor(nonexistentServiceName2, someWaitStrategy);

        Throwable thrownWhenRequestedToWaitForNonexistentService = catchThrowable(environment::start);

        assertThat(thrownWhenRequestedToWaitForNonexistentService)
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(nonexistentServiceName1, nonexistentServiceName2)
            .hasMessageNotContaining(existentServiceName);
    }
}
