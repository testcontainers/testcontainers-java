package org.testcontainers.junit.wait;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.rnorth.ducttape.RetryCountExceededException;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.HttpWaitStrategy;
import org.testcontainers.containers.wait.SimpleWaitStrategy;

import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tests for {@link HttpWaitStrategy}.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class SimpleWaitStrategyTest extends AbstractWaitStrategyTest<SimpleWaitStrategy> {

    /**
     * Dummy implementation of {@link SimpleWaitStrategy.ContainerReadyCheckFunction} checks for socket connection to container .
     */
    private SimpleWaitStrategy.ContainerReadyCheckFunction containerReadyCheckFunction = (container, containerLogger) -> {
        containerLogger.info("wait for socket connection to " + container.getContainerIpAddress() + ":" + 8080);
        new Socket(container.getContainerIpAddress(), container.getMappedPort(8080)).close();
        return true;
    };

    /**
     * Expects that the WaitStrategy returns successfully after {@link #containerReadyCheckFunction} did return true.
     *
     * @throws Exception
     */
    @Test
    public void testWaitUntilReady_Success() throws Exception {
        waitUntilReadyAndSucceed("nc -lp 8080");
    }

    /**
     * Expects that the WaitStrategy throws a {@link RetryCountExceededException} after {@link #containerReadyCheckFunction} didn't return true
     * within the timeout period.
     *
     * @throws Exception
     */
    @Test
    public void testWaitUntilReady_Timeout() throws Exception {
        waitUntilReadyAndTimeout("");
    }

    /**
     * @param ready the AtomicBoolean on which to indicate success
     * @return the WaitStrategy under test
     */
    @NotNull
    protected SimpleWaitStrategy buildWaitStrategy(final AtomicBoolean ready) {

        return new SimpleWaitStrategy("socket connection", containerReadyCheckFunction) {
            @Override
            public void waitUntilReady(GenericContainer container) {
                // blocks until ready or timeout occurs
                super.waitUntilReady(container);
                ready.set(true);
            }
        };
    }
}
