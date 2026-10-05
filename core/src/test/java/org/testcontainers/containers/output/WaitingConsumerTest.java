package org.testcontainers.containers.output;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Timeout(5)
class WaitingConsumerTest {

    @Test
    void waitsUntilEndWithMaximumTimeout() {
        WaitingConsumer consumer = new WaitingConsumer();
        consumer.accept(OutputFrame.END);

        assertThatCode(() -> consumer.waitUntilEnd(Long.MAX_VALUE, TimeUnit.NANOSECONDS)).doesNotThrowAnyException();
    }

    @Test
    void waitsUntilEndWithSaturatedTimeout() {
        WaitingConsumer consumer = new WaitingConsumer();
        consumer.accept(OutputFrame.END);

        assertThatCode(() -> consumer.waitUntilEnd(Long.MAX_VALUE, TimeUnit.SECONDS)).doesNotThrowAnyException();
    }

    @Test
    void waitsUntilEndWithoutTimeout() {
        WaitingConsumer consumer = new WaitingConsumer();
        consumer.accept(OutputFrame.END);

        assertThatCode(consumer::waitUntilEnd).doesNotThrowAnyException();
    }

    @Test
    void waitsUntilEndWithFiniteTimeout() {
        WaitingConsumer consumer = new WaitingConsumer();
        consumer.accept(OutputFrame.END);

        assertThatCode(() -> consumer.waitUntilEnd(1, TimeUnit.SECONDS)).doesNotThrowAnyException();
    }

    @Test
    void timesOutWhenEndIsNotReceived() {
        WaitingConsumer consumer = new WaitingConsumer();

        assertThatThrownBy(() -> consumer.waitUntilEnd(1, TimeUnit.MILLISECONDS))
            .isInstanceOf(TimeoutException.class)
            .hasMessage("Expiry time reached before end of output");
    }
}
