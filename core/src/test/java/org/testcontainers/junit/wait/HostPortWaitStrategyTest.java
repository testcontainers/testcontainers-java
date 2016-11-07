package org.testcontainers.junit.wait;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.rnorth.ducttape.RetryCountExceededException;
import org.testcontainers.containers.wait.HostPortWaitStrategy;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tests for {@link HostPortWaitStrategy}.
 *
 * @author Pete Cornish {@literal <outofcoffee@gmail.com>}
 */
public class HostPortWaitStrategyTest extends AbstractWaitStrategyTest<HostPortWaitStrategy> {
    /**
     * Expects that the WaitStrategy returns successfully after connection to a container with a listening port.
     */
    @Test
    public void testWaitUntilReady_Success() {
        waitUntilReadyAndSucceed("while true; do nc -lp 8080; done");
    }

    /**
     * Expects that the WaitStrategy throws a {@link RetryCountExceededException} after unsuccessful connection
     * to a container with a listening port within the timeout period.
     */
    @Test
    public void testWaitUntilReady_Timeout() {
        waitUntilReadyAndTimeout("exit 1");
    }

    /**
     * @param ready the AtomicBoolean on which to indicate success
     * @return the WaitStrategy under test
     */
    @NotNull
    protected HostPortWaitStrategy buildWaitStrategy(final AtomicBoolean ready) {
        return new HostPortWaitStrategy() {
            @Override
            protected void waitUntilReady() {
                // blocks until ready or timeout occurs
                super.waitUntilReady();
                ready.set(true);
            }
        };
    }
}
