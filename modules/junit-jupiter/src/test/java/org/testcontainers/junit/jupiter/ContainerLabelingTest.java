package org.testcontainers.junit.jupiter;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ContainerLabelingTest {

    private static final DockerImageName IMAGE = DockerImageName.parse("nginx:1.27-alpine");

    @Testcontainers
    static class LabeledContainerTest {

        @Container
        @SuppressWarnings("unused")
        static GenericContainer<?> container = new GenericContainer<>(IMAGE)
            .withExposedPorts(80)
            .waitingFor(new HostPortWaitStrategy());

        @Test
        void shouldHaveTestClassLabel() {
            Map<String, String> labels = container.getContainerInfo().getConfig().getLabels();
            assertThat(labels)
                .containsEntry("testcontainers/test-class",
                    ContainerLabelingTest.class.getName() + "$LabeledContainerTest")
                .containsEntry("testcontainers/test-field", "container");
        }
    }
}
