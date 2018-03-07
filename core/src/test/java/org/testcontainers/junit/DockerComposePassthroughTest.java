package org.testcontainers.junit;

import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.ContainerState;
import org.testcontainers.containers.DockerComposeContainer;
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy;
import org.testcontainers.utility.TestEnvironment;

import java.io.File;

import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
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
        assertEquals("Environment variable set correctly", "bar", container.getEnvMap().get("bar"));

        //check other container properties
        assertNotNull("Container id is not null", container.getContainerId());
        assertThat("Container name", container.getContainerName(), endsWith("alpine_1"));
        assertNotNull("Port mapped", container.getMappedPort(3000));
        assertThat("Exposed Ports", container.getExposedPorts(), hasItem(3000));
        assertEquals("Command is as expected", "/bin/sh -c /passthrough.sh", String.join(" ", container.getCommandParts()));
        assertThat("Image name", container.getDockerImageName(), endsWith("alpine:latest"));
        assertThat("Volume", container.getBinds().get(0).getVolume().getPath(), endsWith("/data"));

    }

    /*
     * WaitStrategy is the only class that has access to the DockerComposeServiceInstance reference
     * Using a custom WaitStrategy to expose the reference for testability
     */
    class TestWaitStrategy extends HostPortWaitStrategy {

        @SuppressWarnings("unchecked")
        public ContainerState getContainer() {
            return this.container;
        }
    }
}
