package org.testcontainers.junit;

import org.junit.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.utility.TestEnvironment;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

/**
 * Simple tests of named network modes - more may be possible, but may not be reproducible
 * without other setup steps.
 */
public class DockerNetworkModeTest {

    @BeforeClass
    public static void checkVersion() {
        Assume.assumeTrue(TestEnvironment.dockerApiAtLeast("1.22"));
    }

    @ClassRule
    public static GenericContainer noNetwork = new GenericContainer("alpine:3.2")
            .withNetworkMode("none")
            .withCommand("ping -c 5 www.google.com");

    @ClassRule
    public static GenericContainer hostNetwork = new GenericContainer("alpine:3.2")
            .withNetworkMode("host")
            .withCommand("ping -c 5 www.google.com");

    @ClassRule
    public static GenericContainer bridgedNetwork = new GenericContainer("alpine:3.2")
            .withNetworkMode("bridge")
            .withCommand("ping -c 5 www.google.com");

    @Test
    public void testNoNetworkContainer() throws TimeoutException {
        String output = getContainerOutput(noNetwork);

        assertTrue("'none' network causes a network access error", output.contains("bad address"));
    }

    @Test
    public void testHostNetworkContainer() throws TimeoutException {
        String output = getContainerOutput(hostNetwork);

        assertTrue("'host' network can access the internet", output.contains("seq=1"));
    }

    @Test
    public void testBridgedNetworkContainer() throws TimeoutException {
        String output = getContainerOutput(bridgedNetwork);

        assertTrue("'bridge' network can access the internet", output.contains("seq=1"));
    }

    private String getContainerOutput(GenericContainer container) throws TimeoutException {
        WaitingConsumer waitingConsumer = new WaitingConsumer();
        ToStringConsumer toStringConsumer = new ToStringConsumer();
        Consumer<OutputFrame> composedConsumer = waitingConsumer.andThen(toStringConsumer);

        container.followOutput(composedConsumer);
        waitingConsumer.waitUntilEnd(10, TimeUnit.SECONDS);

        return toStringConsumer.toUtf8String();
    }
}
