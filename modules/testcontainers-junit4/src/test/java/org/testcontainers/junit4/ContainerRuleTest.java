package org.testcontainers.junit4;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

public class ContainerRuleTest {

    private static final DockerImageName TINY_IMAGE = DockerImageName.parse("alpine:3.16");

    private static GenericContainer<?> staticContainer = new GenericContainer<>(TINY_IMAGE).withCommand("top");

    private GenericContainer<?> container = new GenericContainer<>(TINY_IMAGE).withCommand("top");

    @ClassRule
    public static ContainerRule<GenericContainer<?>> getStaticContainer() {
        return new ContainerRule<>(staticContainer);
    }

    @Rule
    public ContainerRule<GenericContainer<?>> getContainer() {
        return new ContainerRule<>(container);
    }

    @ClassRule
    public static ContainerRule<GenericContainer<?>> staticContainerRule = new ContainerRule<>(
        new GenericContainer<>(TINY_IMAGE).withCommand("top")
    );

    @Rule
    public ContainerRule<GenericContainer<?>> containerRule = new ContainerRule<>(
        new GenericContainer<>(TINY_IMAGE).withCommand("top")
    );

    @Test
    public void test() {
        assertThat(staticContainerRule.get().isRunning()).isTrue();
        assertThat(containerRule.get().isRunning()).isTrue();
        assertThat(staticContainer.isRunning()).isTrue();
        assertThat(container.isRunning()).isTrue();
    }
}
