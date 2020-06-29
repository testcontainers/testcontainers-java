package org.testcontainers.junit;

import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.rnorth.visibleassertions.VisibleAssertions.assertFalse;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThrows;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;
import static org.testcontainers.TestImages.ALPINE_IMAGE;
import static org.testcontainers.containers.output.OutputFrame.OutputType.STDOUT;

@Slf4j
public class OutputStreamWithTTYTest {

    @Rule
    public GenericContainer<?> container = new GenericContainer<>(ALPINE_IMAGE)
        .withCommand("ls -1")
        .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
        .withCreateContainerCmdModifier(command -> command.withTty(true));

    @Rule
    public Timeout globalTimeout = Timeout.seconds(10);

    @Test
    public void testFetchStdout() throws TimeoutException {
        WaitingConsumer consumer = new WaitingConsumer();

        container.followOutput(consumer, STDOUT);

        consumer.waitUntil(frame -> frame.getType() == STDOUT && frame.getUtf8String().contains("home"), 4, TimeUnit.SECONDS);
    }

    @Test
    public void testFetchStdoutWithTimeout() {
        WaitingConsumer consumer = new WaitingConsumer();

        container.followOutput(consumer, STDOUT);

        assertThrows("a TimeoutException should be thrown", TimeoutException.class, () -> {
            consumer.waitUntil(frame -> frame.getType() == STDOUT && frame.getUtf8String().contains("qqq"), 1, TimeUnit.SECONDS);
            return true;
        });
    }

    @Test
    public void testFetchStdoutWithNoLimit() throws TimeoutException {
        WaitingConsumer consumer = new WaitingConsumer();

        container.followOutput(consumer, STDOUT);

        consumer.waitUntil(frame -> frame.getType() == STDOUT && frame.getUtf8String().contains("home"));
    }

    @Test
    public void testLogConsumer() throws TimeoutException {
        WaitingConsumer waitingConsumer = new WaitingConsumer();
        Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log);

        Consumer<OutputFrame> composedConsumer = logConsumer.andThen(waitingConsumer);
        container.followOutput(composedConsumer);

        waitingConsumer.waitUntil(frame -> frame.getType() == STDOUT && frame.getUtf8String().contains("home"));
    }

    @Test
    public void testToStringConsumer() throws TimeoutException {
        WaitingConsumer waitingConsumer = new WaitingConsumer();
        ToStringConsumer toStringConsumer = new ToStringConsumer();

        Consumer<OutputFrame> composedConsumer = toStringConsumer.andThen(waitingConsumer);
        container.followOutput(composedConsumer);

        waitingConsumer.waitUntilEnd(4, TimeUnit.SECONDS);

        String utf8String = toStringConsumer.toUtf8String();
        assertTrue("the expected first value was found", utf8String.contains("home"));
        assertTrue("the expected last value was found", utf8String.contains("media"));
        assertFalse("a non-expected value was found", utf8String.contains("qqq"));
    }
}
