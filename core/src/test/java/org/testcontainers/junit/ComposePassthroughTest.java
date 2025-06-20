package org.testcontainers.junit;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.ComposeContainer;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.TestEnvironment;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

@Testcontainers
public class ComposePassthroughTest {

    private final TestWaitStrategy waitStrategy = new TestWaitStrategy();

    @BeforeAll
    public static void checkVersion() {
        assumeThat(TestEnvironment.dockerApiAtLeast("1.22"))
            .as("dockerApiAtLeast(\"1.22\")")
            .isTrue();
    }

    @Container
    public ComposeContainer compose = new ComposeContainer(
        new File("src/test/resources/v2-compose-test-passthrough.yml")
    )
        .withEnv("foo", "bar")
        .withExposedService("alpine-1", 3000, waitStrategy);

    @Test
    public void testContainerInstanceProperties() {
        final ContainerState container = waitStrategy.getContainer();

        //check environment variable was set
        assertThat(Arrays.asList(Objects.requireNonNull(container.getContainerInfo().getConfig().getEnv())))
            .as("Environment variable set correctly")
            .containsOnlyOnce("bar=bar");

        //check other container properties
        assertThat(container.getContainerId()).as("Container id is not null").isNotNull();
        assertThat(container.getMappedPort(3000)).as("Port mapped").isNotNull();
        assertThat(container.getExposedPorts()).containsExactly(3000);
    }

    /*
     * WaitStrategy is the only class that has access to the DockerComposeServiceInstance reference
     * Using a custom WaitStrategy to expose the reference for testability
     */
    class TestWaitStrategy extends HostPortWaitStrategy {

        @SuppressWarnings("unchecked")
        public ContainerState getContainer() {
            return this.waitStrategyTarget;
        }
    }
}
