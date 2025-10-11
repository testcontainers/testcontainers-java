package org.testcontainers.containers;

import org.junit.jupiter.api.Test;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class DockerComposeContainerCustomImageTest {

    public static final String DOCKER_IMAGE = "docker/compose:debian-1.29.2";

    private static final String COMPOSE_FILE_PATH = "src/test/resources/scaled-compose-test.yml";

    @Test
    void testWithCustomDockerImage() {
        DockerComposeContainer<?> composeContainer = new DockerComposeContainer<>(
            DockerImageName.parse(DOCKER_IMAGE),
            "testing",
            new File(COMPOSE_FILE_PATH)
        );
        composeContainer.start();
        verifyContainerCreation(composeContainer);
        composeContainer.stop();
    }

    @Test
    void testWithCustomDockerImageAndIdentifier() {
        DockerComposeContainer<?> composeContainer = new DockerComposeContainer(
            DockerImageName.parse(DOCKER_IMAGE),
            "myidentifier",
            new File(COMPOSE_FILE_PATH)
        );
        composeContainer.start();
        verifyContainerCreation(composeContainer);
        composeContainer.stop();
    }

    private void verifyContainerCreation(DockerComposeContainer<?> composeContainer) {
        Optional<ContainerState> redis = composeContainer.getContainerByServiceName("redis");
        assertThat(redis)
            .hasValueSatisfying(container -> {
                assertThat(container.isRunning()).isTrue();
                assertThat(container.getContainerInfo().getConfig().getLabels())
                    .containsEntry("com.docker.compose.version", "1.29.2");
            });
    }
}
