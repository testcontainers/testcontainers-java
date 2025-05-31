package org.testcontainers.junit;

import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.testcontainers.TestImages;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.OutputFrame;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.output.ToStringConsumer;
import org.testcontainers.containers.output.WaitingConsumer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.junit4.TestcontainersRule;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@Slf4j
public class OutputStreamWithTTYTest {

    @Rule
    public TestcontainersRule<GenericContainer<?>> container = new TestcontainersRule<>(
        new GenericContainer<>(TestImages.ALPINE_IMAGE)
            .withCommand("ls -1")
            .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
            .withCreateContainerCmdModifier(command -> command.withTty(true))
    );

    @Rule
    public Timeout globalTimeout = Timeout.seconds(10);

    @Test
    public void testFetchStdout() throws TimeoutException {
        WaitingConsumer consumer = new WaitingConsumer();

        container.get().followOutput(consumer, OutputFrame.OutputType.STDOUT);

        consumer.waitUntil(
            frame -> {
                return frame.getType() == OutputFrame.OutputType.STDOUT && frame.getUtf8String().contains("home");
            },
            4,
            TimeUnit.SECONDS
        );
    }

    @Test
    public void testFetchStdoutWithTimeout() {
        WaitingConsumer consumer = new WaitingConsumer();

        container.get().followOutput(consumer, OutputFrame.OutputType.STDOUT);

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
    public void testFetchStdoutWithNoLimit() throws TimeoutException {
        WaitingConsumer consumer = new WaitingConsumer();

        container.get().followOutput(consumer, OutputFrame.OutputType.STDOUT);

        consumer.waitUntil(frame -> {
            return frame.getType() == OutputFrame.OutputType.STDOUT && frame.getUtf8String().contains("home");
        });
    }

    @Test
    public void testLogConsumer() throws TimeoutException {
        WaitingConsumer waitingConsumer = new WaitingConsumer();
        Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log);

        Consumer<OutputFrame> composedConsumer = logConsumer.andThen(waitingConsumer);
        container.get().followOutput(composedConsumer);

        waitingConsumer.waitUntil(frame -> {
            return frame.getType() == OutputFrame.OutputType.STDOUT && frame.getUtf8String().contains("home");
        });
    }

    @Test
    public void testToStringConsumer() throws TimeoutException {
        WaitingConsumer waitingConsumer = new WaitingConsumer();
        ToStringConsumer toStringConsumer = new ToStringConsumer();

        Consumer<OutputFrame> composedConsumer = toStringConsumer.andThen(waitingConsumer);
        container.get().followOutput(composedConsumer);

        waitingConsumer.waitUntilEnd(4, TimeUnit.SECONDS);

        String utf8String = toStringConsumer.toUtf8String();
        assertThat(utf8String).as("the expected first value was found").contains("home");
        assertThat(utf8String).as("the expected last value was found").contains("media");
        assertThat(utf8String).as("a non-expected value was found").doesNotContain("qqq");
    }
}
