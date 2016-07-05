package org.testcontainers.junit.wait;

import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.rnorth.ducttape.RetryCountExceededException;
import org.rnorth.visibleassertions.VisibleAssertions;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.WaitStrategy;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;

/**
 * Common test methods for {@link WaitStrategy} implementations.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public abstract class AbstractWaitStrategyTest<W extends WaitStrategy> {
    private static final long WAIT_TIMEOUT_MILLIS = 3000;
    private static final String IMAGE_NAME = "alpine:latest";

    /**
     * Indicates that the WaitStrategy has completed waiting successfully.
     */
    private AtomicBoolean ready;

    /**
     * Subclasses should return an instance of {@link W} that sets {@code ready} to {@code true},
     * if the wait was successful.
     *
     * @param ready the AtomicBoolean on which to indicate success
     * @return WaitStrategy implementation
     */
    @NotNull
    protected abstract W buildWaitStrategy(final AtomicBoolean ready);

    @Before
    public void setUp() throws Exception {
        ready = new AtomicBoolean(false);
    }

    /**
     * Starts a GenericContainer with the {@link #IMAGE_NAME} image, passing the given {@code shellCommand} as
     * a parameter to {@literal sh -c} (the container CMD).
     *
     * @param shellCommand the shell command to execute
     * @return the (unstarted) container
     */
    private GenericContainer startContainerWithCommand(String shellCommand) {
        final WaitStrategy waitStrategy = buildWaitStrategy(ready)
                .withStartupTimeout(Duration.ofMillis(WAIT_TIMEOUT_MILLIS));

        // apply WaitStrategy to container
        return new GenericContainer(IMAGE_NAME)
                .withExposedPorts(8080)
                .withCommand("sh", "-c", shellCommand)
                .waitingFor(waitStrategy);
    }

    /**
     * Expects that the WaitStrategy returns successfully after connection to a container with a listening port.
     *
     * @param shellCommand the shell command to execute
     */
    protected void waitUntilReadyAndSucceed(String shellCommand) {
        final GenericContainer container = startContainerWithCommand(shellCommand);

        // start() blocks until successful or timeout
        container.start();

        assertTrue(String.format("Expected container to be ready after timeout of %sms",
                WAIT_TIMEOUT_MILLIS), ready.get());
    }

    /**
     * Expects that the WaitStrategy throws a {@link RetryCountExceededException} after unsuccessful connection
     * to a container with a listening port.
     *
     * @param shellCommand the shell command to execute
     */
    protected void waitUntilReadyAndTimeout(String shellCommand) {
        final GenericContainer container = startContainerWithCommand(shellCommand);

        // start() blocks until successful or timeout
        VisibleAssertions.assertThrows("an exception is thrown when timeout occurs (" + WAIT_TIMEOUT_MILLIS + "ms)",
                ContainerLaunchException.class,
                container::start);

    }
}
