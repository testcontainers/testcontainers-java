package com.example.compose;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.ContainerState;

import java.util.Optional;

import static org.junit.Assert.assertTrue;

public class DockerComposeTestOne extends DockerComposeIntegrationTest {

    @Test
    public void test_one() {
        Optional<ContainerState> container = DOCKER_COMPOSE_CONTAINER.getContainerByServiceName("redis");
        assertTrue(container.isPresent());
        assertTrue(container.get().isRunning());
    }

}
