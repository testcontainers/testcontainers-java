package org.testcontainers.containers;

import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ComposeContainerTest {

    public static final String DOCKER_IMAGE = "docker:25.0.2";

    private static final String COMPOSE_FILE_PATH = "src/test/resources/v2-compose-test.yml";

    @Test
    void testWithCustomDockerImage() {
        ComposeContainer composeContainer = new ComposeContainer(
            DockerImageName.parse(DOCKER_IMAGE),
            new File(COMPOSE_FILE_PATH)
        );
        composeContainer.start();
        verifyContainerCreation(composeContainer);
        composeContainer.stop();
    }

    @Test
    void testWithCustomDockerImageAndIdentifier() {
        ComposeContainer composeContainer = new ComposeContainer(
            DockerImageName.parse(DOCKER_IMAGE),
            "myidentifier",
            new File(COMPOSE_FILE_PATH)
        );
        composeContainer.start();
        verifyContainerCreation(composeContainer);
        composeContainer.stop();
    }

    private void verifyContainerCreation(ComposeContainer composeContainer) {
        Optional<ContainerState> redis = composeContainer.getContainerByServiceName("redis");
        assertThat(redis)
            .hasValueSatisfying(container -> {
                assertThat(container.isRunning()).isTrue();
                assertThat(container.getContainerInfo().getConfig().getLabels())
                    .containsEntry("com.docker.compose.version", "2.24.5");
            });
    }
}
