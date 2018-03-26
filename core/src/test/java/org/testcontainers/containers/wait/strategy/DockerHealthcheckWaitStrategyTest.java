package org.testcontainers.containers.wait.strategy;

import org.junit.Before;
import org.junit.Test;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.time.Duration;

import static org.rnorth.visibleassertions.VisibleAssertions.assertThrows;

public class DockerHealthcheckWaitStrategyTest {

    private GenericContainer container;

    @Before
    public void setUp() {
        // Using a Dockerfile here, since Dockerfile builder DSL doesn't support HEALTHCHECK
        container = new GenericContainer(new ImageFromDockerfile()
            .withFileFromClasspath("write_file_and_loop.sh", "health-wait-strategy-dockerfile/write_file_and_loop.sh")
            .withFileFromClasspath("Dockerfile", "health-wait-strategy-dockerfile/Dockerfile"))
            .waitingFor(Wait.forHealthcheck().withStartupTimeout(Duration.ofSeconds(3)));
    }

    @Test
    public void startsOnceHealthy() {
        container.start();
    }

    @Test
    public void containerStartFailsIfContainerIsUnhealthy() {
        container.withCommand("tail", "-f", "/dev/null");
        assertThrows("Container launch fails when unhealthy", ContainerLaunchException.class, container::start);
    }
}
