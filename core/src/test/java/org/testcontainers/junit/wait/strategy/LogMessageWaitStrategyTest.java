package org.testcontainers.junit.wait.strategy;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Tests for {@link LogMessageWaitStrategy}.
 */
public class LogMessageWaitStrategyTest extends AbstractWaitStrategyTest<LogMessageWaitStrategy> {

    private String pattern;

    public static Stream<Arguments> providePatterns() {
        return Stream.of(
            Arguments.of(".*ready.*\\s"), // previous recommended style (explicit line ending)
            Arguments.of(".*ready!\\s"), // explicit line ending without wildcard after expected text
            Arguments.of(".*ready.*") // new style (line ending matched by wildcard)
        );
    }

    private static final String READY_MESSAGE = "I'm ready!";

    @ParameterizedTest
    @MethodSource("providePatterns")
    public void testWaitUntilReady_Success(String pattern) {
        this.pattern = pattern;

        waitUntilReadyAndSucceed(
            "echo -e \"" +
            READY_MESSAGE +
            "\";" +
            "echo -e \"foobar\";" +
            "echo -e \"" +
            READY_MESSAGE +
            "\";" +
            "sleep 300"
        );
    }

    @Test
    public void testWaitUntilReady_Timeout() {
        waitUntilReadyAndTimeout("echo -e \"" + READY_MESSAGE + "\";" + "echo -e \"foobar\";" + "sleep 300");
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
        }
            .withRegEx(pattern)
            .withTimes(2);
    }
}
