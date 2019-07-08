package org.testcontainers.junit;

import com.github.dockerjava.api.DockerClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.*;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.containers.startupcheck.StartupCheckStrategy;
import org.testcontainers.utility.TestEnvironment;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

/**
 * Simple tests of named network modes - more may be possible, but may not be reproducible
 * without other setup steps.
 */
@Slf4j
public class DockerNetworkModeTest {

    @BeforeClass
    public static void checkVersion() {
        Assume.assumeTrue(TestEnvironment.dockerApiAtLeast("1.22"));
    }

    @Test
    public void testNoNetworkContainer() throws TimeoutException {
        try (GenericContainer container = pingingContainer().withNetworkMode("none")) {
            container.start();
            String output = getContainerOutput(container);

            assertTrue("'none' network causes a network access error", output.contains("bad address"));
        }
    }

    @Test
    public void testHostNetworkContainer() throws TimeoutException {
        try (GenericContainer container = pingingContainer().withNetworkMode("host")) {
            container.start();
            String output = getContainerOutput(container);

            assertTrue("'host' network can access the internet", output.contains("seq=0"));
        }
    }

    @Test
    public void testBridgedNetworkContainer() throws TimeoutException {
        try (GenericContainer container = pingingContainer().withNetworkMode("bridge")) {
            container.start();
            String output = getContainerOutput(container);

            assertTrue("'bridge' network can access the internet", output.contains("seq=0"));
        }
    }

    private GenericContainer<?> pingingContainer() {
        return new GenericContainer<>()
            .withStartupCheckStrategy(new StartupCheckStrategy() {
                @Override
                public StartupStatus checkStartupState(DockerClient dockerClient, String containerId) {
                    return StartupStatus.SUCCESSFUL;
                }
            })
            .withLogConsumer(new Slf4jLogConsumer(log))
            .withCommand("ping -c 1 -w 1 testcontainers.org");
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
