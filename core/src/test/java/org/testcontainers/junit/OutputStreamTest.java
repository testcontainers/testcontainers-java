package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.output.WaitingConsumer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.rnorth.visibleassertions.VisibleAssertions.assertFalse;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThrows;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;
import static org.testcontainers.TestImages.ALPINE_IMAGE;
import static org.testcontainers.containers.output.OutputFrame.OutputType.STDOUT;

/**
 * Simple test for following container output.
 */
public class OutputStreamTest {

    @Rule
    public GenericContainer container = new GenericContainer(ALPINE_IMAGE)
            .withCommand("ping -c 5 127.0.0.1");

    private static final Logger LOGGER = LoggerFactory.getLogger(OutputStreamTest.class);

    @Test(timeout = 60_000L)
    public void testFetchStdout() throws TimeoutException {

        WaitingConsumer consumer = new WaitingConsumer();

        container.followOutput(consumer, STDOUT);

        consumer.waitUntil(frame -> frame.getType() == STDOUT && frame.getUtf8String().contains("seq=2"),
                30, TimeUnit.SECONDS);
    }

    @Test(timeout = 60_000L)
    public void testFetchStdoutWithTimeout() {

        WaitingConsumer consumer = new WaitingConsumer();

        container.followOutput(consumer, STDOUT);

        assertThrows("a TimeoutException should be thrown", TimeoutException.class, () -> {
            consumer.waitUntil(frame -> frame.getType() == STDOUT && frame.getUtf8String().contains("seq=5"),
                    2, TimeUnit.SECONDS);
            return true;
        });
    }

    @Test(timeout = 60_000L)
    public void testFetchStdoutWithNoLimit() throws TimeoutException {

        WaitingConsumer consumer = new WaitingConsumer();

        container.followOutput(consumer, STDOUT);

        consumer.waitUntil(frame -> frame.getType() == STDOUT && frame.getUtf8String().contains("seq=2"));
    }

    @Test(timeout = 60_000L)
    public void testLogConsumer() throws TimeoutException {

        WaitingConsumer waitingConsumer = new WaitingConsumer();
        Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(LOGGER);

        Consumer<OutputFrame> composedConsumer = logConsumer.andThen(waitingConsumer);
        container.followOutput(composedConsumer);

        waitingConsumer.waitUntil(frame -> frame.getType() == STDOUT && frame.getUtf8String().contains("seq=2"));
    }

    @Test(timeout = 60_000L)
    public void testToStringConsumer() throws TimeoutException {

        WaitingConsumer waitingConsumer = new WaitingConsumer();
        ToStringConsumer toStringConsumer = new ToStringConsumer();

        Consumer<OutputFrame> composedConsumer = toStringConsumer.andThen(waitingConsumer);
        container.followOutput(composedConsumer);

        waitingConsumer.waitUntilEnd(30, TimeUnit.SECONDS);

        String utf8String = toStringConsumer.toUtf8String();
        assertTrue("the expected first value was found", utf8String.contains("seq=1"));
        assertTrue("the expected last value was found", utf8String.contains("seq=4"));
        assertFalse("a non-expected value was found", utf8String.contains("seq=42"));
    }
}

