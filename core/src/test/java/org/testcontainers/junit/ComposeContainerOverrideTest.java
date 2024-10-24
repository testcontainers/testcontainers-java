package org.testcontainers.junit;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.junit.Test;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.ContainerState;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;

public class ComposeContainerOverrideTest {

    private static final File BASE = new File("src/test/resources/compose-override/compose.yml");

    private static final File OVERRIDE = new File("src/test/resources/compose-override/compose-override.yml");

    @Test
    public void readEnvironment() {
        try (ComposeContainer compose = new ComposeContainer(BASE).withExposedService("redis", 6379)) {
            compose.start();
            InspectContainerResponse container = compose
                .getContainerByServiceName("redis-1")
                .map(ContainerState::getContainerInfo)
                .get();
            assertThat(container.getConfig().getEnv()).contains("foo=bar");
        }
    }

    @Test
    public void resetEnvironment() {
        try (ComposeContainer compose = new ComposeContainer(BASE, OVERRIDE).withExposedService("redis", 6379)) {
            compose.start();
            InspectContainerResponse container = compose
                .getContainerByServiceName("redis-1")
                .map(ContainerState::getContainerInfo)
                .get();
            assertThat(container.getConfig().getEnv()).doesNotContain("foo=bar");
        }
    }
}
