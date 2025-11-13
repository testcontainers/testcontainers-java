package org.testcontainers.junit;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.utility.DockerImageName;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

class ComposeContainerOverrideTest {

    private static final File BASE = new File("src/test/resources/compose-override/compose.yml");

    private static final File OVERRIDE = new File("src/test/resources/compose-override/compose-override.yml");

    @Test
    void readEnvironment() {
        try (
            ComposeContainer compose = new ComposeContainer(DockerImageName.parse("docker:25.0.5"), BASE)
                .withExposedService("redis", 6379)
        ) {
            compose.start();
            InspectContainerResponse container = compose
                .getContainerByServiceName("redis-1")
                .map(ContainerState::getContainerInfo)
                .get();
            assertThat(container.getConfig().getEnv()).contains("foo=bar");
        }
    }

    @Test
    void resetEnvironment() {
        try (
            ComposeContainer compose = new ComposeContainer(DockerImageName.parse("docker:25.0.5"), BASE, OVERRIDE)
                .withExposedService("redis", 6379)
        ) {
            compose.start();
            InspectContainerResponse container = compose
                .getContainerByServiceName("redis-1")
                .map(ContainerState::getContainerInfo)
                .get();
            assertThat(container.getConfig().getEnv()).doesNotContain("foo=bar");
        }
    }
}
