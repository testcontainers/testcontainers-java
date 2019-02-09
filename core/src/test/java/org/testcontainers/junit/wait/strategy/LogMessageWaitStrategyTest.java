package org.testcontainers.junit.wait.strategy;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Tests for {@link LogMessageWaitStrategy}.
 */
@RunWith(Parameterized.class)
public class LogMessageWaitStrategyTest extends AbstractWaitStrategyTest<LogMessageWaitStrategy> {

    private final String pattern;

    @Parameterized.Parameters(name = "{0}")
    public static Object[] parameters() {
        return new String[] {
            ".*ready.*\\s",     // previous recommended style (explicit line ending)
            ".*ready!\\s",      // explicit line ending without wildcard after expected text
            ".*ready.*"         // new style (line ending matched by wildcard)
        };
    }

    public LogMessageWaitStrategyTest(String pattern) {
        this.pattern = pattern;
    }

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
        }.withRegEx(pattern).withTimes(2);
    }
}
