package org.testcontainers.junit;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.utility.TestEnvironment;

import java.io.File;
import java.util.Arrays;
import java.util.Objects;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.rnorth.visibleassertions.VisibleAssertions.assertNotNull;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThat;

/**
 * Created by rnorth on 11/06/2016.
 */
public class DockerComposePassthroughTest {

    private final TestWaitStrategy waitStrategy = new TestWaitStrategy();

    @BeforeClass
    public static void checkVersion() {
        Assume.assumeTrue(TestEnvironment.dockerApiAtLeast("1.22"));
    }

    @Rule
    public DockerComposeContainer compose =
        new DockerComposeContainer(new File("src/test/resources/v2-compose-test-passthrough.yml"))
            .withEnv("foo", "bar")
            .withExposedService("alpine_1", 3000, waitStrategy);


    @Test
    public void testContainerInstanceProperties() {
        final ContainerState container = waitStrategy.getContainer();

        //check environment variable was set
        assertThat("Environment variable set correctly", Arrays.asList(Objects.requireNonNull(container.getContainerInfo()
            .getConfig().getEnv())), hasItem("bar=bar"));

        //check other container properties
        assertNotNull("Container id is not null", container.getContainerId());
        assertNotNull("Port mapped", container.getMappedPort(3000));
        assertThat("Exposed Ports", container.getExposedPorts(), hasItem(3000));

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
