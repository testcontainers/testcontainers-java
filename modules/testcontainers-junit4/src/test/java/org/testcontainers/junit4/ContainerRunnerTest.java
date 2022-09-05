package org.testcontainers.junit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(TestContainersRunner.class)
public class ContainerRunnerTest {

    private static final DockerImageName TINY_IMAGE = DockerImageName.parse("alpine:3.16");

    @ClassContainer
    public static GenericContainer<?> staticContainer = new GenericContainer<>(TINY_IMAGE).withCommand("top");

    @Container
    public GenericContainer<?> container = new GenericContainer<>(TINY_IMAGE).withCommand("top");

    @Test
    public void test() {
        assertThat(staticContainer.isRunning()).isTrue();
        assertThat(container.isRunning()).isTrue();
    }
}
