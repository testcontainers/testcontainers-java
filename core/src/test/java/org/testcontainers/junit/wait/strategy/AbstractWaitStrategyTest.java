package org.testcontainers.junit.wait.strategy;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.rnorth.ducttape.RetryCountExceededException;
import org.testcontainers.TestImages;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Common test methods for {@link WaitStrategy} implementations.
 */
public abstract class AbstractWaitStrategyTest<W extends WaitStrategy> {

    static final long WAIT_TIMEOUT_MILLIS = 3000;

    /**
     * Indicates that the WaitStrategy has completed waiting successfully.
     */
    AtomicBoolean ready;

    /**
     * Subclasses should return an instance of {@link W} that sets {@code ready} to {@code true},
     * if the wait was successful.
     *
     * @param ready the AtomicBoolean on which to indicate success
     * @return WaitStrategy implementation
     */
    @NotNull
    protected abstract W buildWaitStrategy(final AtomicBoolean ready);

    @BeforeEach
    public void setUp() {
        ready = new AtomicBoolean(false);
    }

    /**
     * Starts a GenericContainer with the Alpine image, passing the given {@code shellCommand} as
     * a parameter to {@literal sh -c} (the container CMD).
     *
     * @param shellCommand the shell command to execute
     * @return the (unstarted) container
     */
    private GenericContainer<?> startContainerWithCommand(String shellCommand) {
        return startContainerWithCommand(shellCommand, buildWaitStrategy(ready));
    }

    /**
     * Starts a GenericContainer with the Alpine image, passing the given {@code shellCommand} as
     * a parameter to {@literal sh -c} (the container CMD) and apply a give wait strategy.
     * Note that the timeout will be overwritten if any with {@link #WAIT_TIMEOUT_MILLIS}.
     * @param shellCommand the shell command to execute
     * @param waitStrategy The wait strategy to apply
     * @return the (unstarted) container
     */
    protected GenericContainer<?> startContainerWithCommand(String shellCommand, WaitStrategy waitStrategy) {
        return startContainerWithCommand(shellCommand, waitStrategy, 8080);
    }

    protected GenericContainer<?> startContainerWithCommand(
        String shellCommand,
        WaitStrategy waitStrategy,
        Integer... ports
    ) {
        // apply WaitStrategy to container
        return new GenericContainer<>(TestImages.ALPINE_IMAGE)
            .withExposedPorts(ports)
            .withCommand("sh", "-c", shellCommand)
            .waitingFor(waitStrategy.withStartupTimeout(Duration.ofMillis(WAIT_TIMEOUT_MILLIS)));
    }

    /**
     * Expects that the WaitStrategy returns successfully after connection to a container with a listening port.
     *
     * @param shellCommand the shell command to execute
     */
    protected void waitUntilReadyAndSucceed(String shellCommand) {
        try (GenericContainer<?> container = startContainerWithCommand(shellCommand)) {
            waitUntilReadyAndSucceed(container);
        }
    }

    /**
     * Expects that the WaitStrategy throws a {@link RetryCountExceededException} after unsuccessful connection
     * to a container with a listening port.
     *
     * @param shellCommand the shell command to execute
     */
    protected void waitUntilReadyAndTimeout(String shellCommand) {
        try (GenericContainer<?> container = startContainerWithCommand(shellCommand)) {
            waitUntilReadyAndTimeout(container);
        }
    }

    /**
     * Expects that the WaitStrategy throws a {@link RetryCountExceededException} after unsuccessful connection
     * to a container with a listening port.
     *
     * @param container the container to start
     */
    protected void waitUntilReadyAndTimeout(GenericContainer<?> container) {
        // start() blocks until successful or timeout
        assertThat(catchThrowable(container::start))
            .as("an exception is thrown when timeout occurs (" + WAIT_TIMEOUT_MILLIS + "ms)")
            .isInstanceOf(ContainerLaunchException.class);
    }

    /**
     * Expects that the WaitStrategy returns successfully after connection to a container with a listening port.
     *
     * @param container the container to start
     */
    protected void waitUntilReadyAndSucceed(GenericContainer<?> container) {
        // start() blocks until successful or timeout
        container.start();

        assertThat(ready)
            .as(String.format("Expected container to be ready after timeout of %sms", WAIT_TIMEOUT_MILLIS))
            .isTrue();
    }
}
