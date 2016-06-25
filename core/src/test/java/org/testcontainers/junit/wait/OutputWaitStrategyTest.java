package org.testcontainers.junit.wait;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.rnorth.ducttape.RetryCountExceededException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.wait.OutputWaitStrategy;
import org.testcontainers.containers.wait.SimpleWaitStrategy;
import org.testcontainers.containers.wait.HttpWaitStrategy;

import java.net.Socket;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * Tests for {@link HttpWaitStrategy}.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class OutputWaitStrategyTest extends AbstractWaitStrategyTest<OutputWaitStrategy> {

    /**
     * Dummy implementation of {@link SimpleWaitStrategy.ContainerReadyCheckFunction} checks for container output frame to be 'READY'.
     */
    private Predicate<OutputFrame> containerOutputFramePredicate = frame -> frame.getUtf8String().trim().equals("READY");

    /**
     * Expects that the WaitStrategy returns successfully after {@link #containerOutputFramePredicate} did return true.
     *
     * @throws Exception
     */
    @Test
    public void testWaitUntilReady_Success() throws Exception {
        waitUntilReadyAndSucceed("echo 'READY'; ping localhost");
    }

    /**
     * Expects that the WaitStrategy throws a {@link RetryCountExceededException} after {@link #containerOutputFramePredicate} didn't return true
     * within the timeout period.
     *
     * @throws Exception
     */
    @Test
    public void testWaitUntilReady_Timeout() throws Exception {
        waitUntilReadyAndTimeout("echo 'BOOT'; ping localhost");
    }

    /**
     * @param ready the AtomicBoolean on which to indicate success
     * @return the WaitStrategy under test
     */
    @NotNull
    protected OutputWaitStrategy buildWaitStrategy(final AtomicBoolean ready) {

        return new OutputWaitStrategy(containerOutputFramePredicate) {
            @Override
            public void waitUntilReady(GenericContainer container) {
                // blocks until ready or timeout occurs
                super.waitUntilReady(container);
                ready.set(true);
            }
        };
    }
}
