package org.testcontainers.junit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.TestImages;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/**
 * Simple test for following container output.
 */
@Testcontainers
public class OutputStreamTest {

    @Container
    public GenericContainer container = new GenericContainer(TestImages.ALPINE_IMAGE)
        .withCommand("ping -c 5 127.0.0.1");

    private static final Logger LOGGER = LoggerFactory.getLogger(OutputStreamTest.class);

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    public void testFetchStdout() throws TimeoutException {
        WaitingConsumer consumer = new WaitingConsumer();

        container.followOutput(consumer, OutputFrame.OutputType.STDOUT);

        consumer.waitUntil(
            frame -> frame.getType() == OutputFrame.OutputType.STDOUT && frame.getUtf8String().contains("seq=2"),
            30,
            TimeUnit.SECONDS
        );
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    public void testFetchStdoutWithTimeout() {
        WaitingConsumer consumer = new WaitingConsumer();

        container.followOutput(consumer, OutputFrame.OutputType.STDOUT);

        assertThat(
            catchThrowable(() -> {
                consumer.waitUntil(
                    frame -> {
                        return (
                            frame.getType() == OutputFrame.OutputType.STDOUT && frame.getUtf8String().contains("seq=5")
                        );
                    },
                    2,
                    TimeUnit.SECONDS
                );
            })
        )
            .as("a TimeoutException should be thrown")
            .isInstanceOf(TimeoutException.class);
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    public void testFetchStdoutWithNoLimit() throws TimeoutException {
        WaitingConsumer consumer = new WaitingConsumer();

        container.followOutput(consumer, OutputFrame.OutputType.STDOUT);

        consumer.waitUntil(frame -> {
            return frame.getType() == OutputFrame.OutputType.STDOUT && frame.getUtf8String().contains("seq=2");
        });
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    public void testLogConsumer() throws TimeoutException {
        WaitingConsumer waitingConsumer = new WaitingConsumer();
        Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(LOGGER);

        Consumer<OutputFrame> composedConsumer = logConsumer.andThen(waitingConsumer);
        container.followOutput(composedConsumer);

        waitingConsumer.waitUntil(frame -> {
            return frame.getType() == OutputFrame.OutputType.STDOUT && frame.getUtf8String().contains("seq=2");
        });
    }

    @Test
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    public void testToStringConsumer() throws TimeoutException {
        WaitingConsumer waitingConsumer = new WaitingConsumer();
        ToStringConsumer toStringConsumer = new ToStringConsumer();

        Consumer<OutputFrame> composedConsumer = toStringConsumer.andThen(waitingConsumer);
        container.followOutput(composedConsumer);

        waitingConsumer.waitUntilEnd(30, TimeUnit.SECONDS);

        String utf8String = toStringConsumer.toUtf8String();
        assertThat(utf8String).as("the expected first value was found").contains("seq=1");
        assertThat(utf8String).as("the expected last value was found").contains("seq=4");
        assertThat(utf8String).as("a non-expected value was found").doesNotContain("seq=42");
    }
}
