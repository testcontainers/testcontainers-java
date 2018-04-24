package org.testcontainers.junit.wait.strategy;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tests for {@link LogMessageWaitStrategy}.
 */
public class LogMessageWaitStrategyTest extends AbstractWaitStrategyTest<LogMessageWaitStrategy> {

    private static final String READY_MESSAGE = "I'm ready!";

    @Test
    public void testWaitUntilReady_Success() {
        waitUntilReadyAndSucceed("echo -e \"" + READY_MESSAGE + "\";" +
                "echo -e \"foobar\";" +
                "echo -e \"" + READY_MESSAGE + "\";" +
                "sleep 300");
    }

    @Test
    public void testWaitUntilReady_Timeout() {
        waitUntilReadyAndTimeout("echo -e \"" + READY_MESSAGE + "\";" +
                "echo -e \"foobar\";" +
                "sleep 300");
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
        }.withRegEx(".*ready.*\\s").withTimes(2);
    }
}
