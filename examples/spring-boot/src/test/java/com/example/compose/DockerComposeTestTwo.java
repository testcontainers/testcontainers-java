package com.example.compose;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.ContainerState;

import java.util.Optional;

import static org.junit.Assert.assertTrue;

public class DockerComposeTestTwo extends DockerComposeIntegrationTest {

    @Test
    public void test_two() {
        Optional<ContainerState> container = DOCKER_COMPOSE_CONTAINER.getContainerByServiceName("redis");
        assertTrue(container.isPresent());
        assertTrue(container.get().isRunning());
    }

}
