package org.testcontainers.junit.jupiter;

import org.junit.Test;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class ComposeFileContainerTests {

    @Test
    public void testContainerNameComposeOptionValidation() {
        try (
            DockerComposeContainer composeContainer = new DockerComposeContainer(
                new File("src/test/resources/docker-compose-with-container-name-option.yml"))
                .withExposedService("whoami_1", 80, Wait.forHttp("/"));
        ) {
            composeContainer.start();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().equals("Compose file src/test/resources/docker-compose-with-container-name-option.yml "
                + "contains 'container_name' option which is not supported by container."));
        }
    }

    @Test
    public void testMissedComposeFile() {
        try (
            DockerComposeContainer composeContainer = new DockerComposeContainer(
                new File("some_compose_file.yml"))
                .withExposedService("whoami_1", 80, Wait.forHttp("/"));
        ) {
            composeContainer.start();
        } catch (ContainerLaunchException e) {
            assertTrue(e.getMessage().equals("Unable to read compose file some_compose_file.yml."));
        }
    }
}
