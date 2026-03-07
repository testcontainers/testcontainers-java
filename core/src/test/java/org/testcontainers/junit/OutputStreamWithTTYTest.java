package org.testcontainers.junit;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.TestImages;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@Slf4j
@Timeout(10)
class OutputStreamWithTTYTest {

    @AutoClose
    public GenericContainer<?> container = new GenericContainer<>(TestImages.ALPINE_IMAGE)
        .withCommand("ls -1")
        .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
        .withCreateContainerCmdModifier(command -> command.withTty(true));

    @BeforeEach
    void setUp() {
        container.start();
    }

    @Test
    void testFetchStdout() throws TimeoutException {
        WaitingConsumer consumer = new WaitingConsumer();

        container.followOutput(consumer, OutputFrame.OutputType.STDOUT);

        consumer.waitUntil(
            frame -> {
                return frame.getType() == OutputFrame.OutputType.STDOUT && frame.getUtf8String().contains("home");
            },
            4,
            TimeUnit.SECONDS
        );
    }

    @Test
    void testFetchStdoutWithTimeout() {
        WaitingConsumer consumer = new WaitingConsumer();

        container.followOutput(consumer, OutputFrame.OutputType.STDOUT);

        assertThat(
            catchThrowable(() -> {
                consumer.waitUntil(
                    frame -> {
                        return (
                            frame.getType() == OutputFrame.OutputType.STDOUT && frame.getUtf8String().contains("qqq")
                        );
                    },
                    1,
                    TimeUnit.SECONDS
                );
            })
        )
            .as("a TimeoutException should be thrown")
            .isInstanceOf(TimeoutException.class);
    }

    @Test
    void testFetchStdoutWithNoLimit() throws TimeoutException {
        WaitingConsumer consumer = new WaitingConsumer();

        container.followOutput(consumer, OutputFrame.OutputType.STDOUT);

        consumer.waitUntil(frame -> {
            return frame.getType() == OutputFrame.OutputType.STDOUT && frame.getUtf8String().contains("home");
        });
    }

    @Test
    void testLogConsumer() throws TimeoutException {
        WaitingConsumer waitingConsumer = new WaitingConsumer();
        Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log);

        Consumer<OutputFrame> composedConsumer = logConsumer.andThen(waitingConsumer);
        container.followOutput(composedConsumer);

        waitingConsumer.waitUntil(frame -> {
            return frame.getType() == OutputFrame.OutputType.STDOUT && frame.getUtf8String().contains("home");
        });
    }

    @Test
    void testToStringConsumer() throws TimeoutException {
        WaitingConsumer waitingConsumer = new WaitingConsumer();
        ToStringConsumer toStringConsumer = new ToStringConsumer();

        Consumer<OutputFrame> composedConsumer = toStringConsumer.andThen(waitingConsumer);
        container.followOutput(composedConsumer);

        waitingConsumer.waitUntilEnd(4, TimeUnit.SECONDS);

        String utf8String = toStringConsumer.toUtf8String();
        assertThat(utf8String).as("the expected first value was found").contains("home");
        assertThat(utf8String).as("the expected last value was found").contains("media");
        assertThat(utf8String).as("a non-expected value was found").doesNotContain("qqq");
    }
}
