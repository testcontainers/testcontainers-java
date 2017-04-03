package org.testcontainers.junit.wait;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.testcontainers.containers.wait.LogMessageWaitStrategy;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tests for {@link LogMessageWaitStrategy}.
 */
public class LogMessageWaitStrategyTest extends AbstractWaitStrategyTest<LogMessageWaitStrategy> {

    private static final String READY_MESSAGE = "I'm ready!";

    @Test
    public void testWaitUntilReady_Success() {
        waitUntilReadyAndSucceed("while true; do sleep 1; echo -e \"" + READY_MESSAGE + "\"; done");
    }

    @Test
    public void testWaitUntilReady_Timeout() {
        waitUntilReadyAndTimeout("while true; do sleep 1; echo -e \"" + "foobar\"; done");
    }

    @NotNull
    @Override
    protected LogMessageWaitStrategy buildWaitStrategy(AtomicBoolean ready) {

        return new LogMessageWaitStrategy() {
            @Override
            protected void waitUntilReady() {
                super.waitUntilReady();
                ready.set(true);
            }
        }.withExpectedLogPart(READY_MESSAGE);
    }
}
