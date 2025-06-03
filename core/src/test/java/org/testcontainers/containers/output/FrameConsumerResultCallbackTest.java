package org.testcontainers.containers.output;

import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public class FrameConsumerResultCallbackTest {

    private static final String FRAME_PAYLOAD =
        "\u001B[0;32mТест1\u001B[0m\n\u001B[1;33mTest2\u001B[0m\n\u001B[0;31mTest3\u001B[0m";

    private static final String LOG_RESULT = "Тест1\nTest2\nTest3";

    @Test
    public void passStderrFrameWithoutColors() throws IOException {
        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        ToStringConsumer consumer = new ToStringConsumer();
        callback.addConsumer(OutputFrame.OutputType.STDERR, consumer);
        callback.onNext(new Frame(StreamType.STDERR, FRAME_PAYLOAD.getBytes()));
        callback.close();
        assertThat(consumer.toUtf8String()).isEqualTo(LOG_RESULT);
    }

    @Test
    public void passStderrFrameWithColors() throws IOException {
        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        ToStringConsumer consumer = new ToStringConsumer().withRemoveAnsiCodes(false);
        callback.addConsumer(OutputFrame.OutputType.STDERR, consumer);
        callback.onNext(new Frame(StreamType.STDERR, FRAME_PAYLOAD.getBytes()));
        callback.close();
        assertThat(consumer.toUtf8String()).isEqualTo(FRAME_PAYLOAD);
    }

    @Test
    public void passStdoutFrameWithoutColors() throws IOException {
        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        ToStringConsumer consumer = new ToStringConsumer();
        callback.addConsumer(OutputFrame.OutputType.STDOUT, consumer);
        callback.onNext(new Frame(StreamType.STDOUT, FRAME_PAYLOAD.getBytes()));
        callback.close();
        assertThat(consumer.toUtf8String()).isEqualTo(LOG_RESULT);
    }

    @Test
    public void passStdoutFrameWithColors() throws IOException {
        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        ToStringConsumer consumer = new ToStringConsumer().withRemoveAnsiCodes(false);
        callback.addConsumer(OutputFrame.OutputType.STDOUT, consumer);
        callback.onNext(new Frame(StreamType.STDOUT, FRAME_PAYLOAD.getBytes()));
        callback.close();
        assertThat(consumer.toUtf8String()).isEqualTo(FRAME_PAYLOAD);
    }

    @Test
    public void basicConsumer() throws IOException {
        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        BasicConsumer consumer = new BasicConsumer();
        callback.addConsumer(OutputFrame.OutputType.STDOUT, consumer);
        callback.onNext(new Frame(StreamType.STDOUT, FRAME_PAYLOAD.getBytes()));
        callback.close();
        assertThat(consumer.toString()).isEqualTo(LOG_RESULT);
    }

    @Test
    public void passStdoutNull() throws IOException {
        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        ToStringConsumer consumer = new ToStringConsumer().withRemoveAnsiCodes(false);
        callback.addConsumer(OutputFrame.OutputType.STDOUT, consumer);
        callback.onNext(new Frame(StreamType.STDOUT, null));
        callback.close();
        assertThat(consumer.toUtf8String()).isEqualTo("");
    }

    @Test
    public void passStdoutEmptyLine() throws IOException {
        String payload = "";
        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        ToStringConsumer consumer = new ToStringConsumer().withRemoveAnsiCodes(false);
        callback.addConsumer(OutputFrame.OutputType.STDOUT, consumer);
        callback.onNext(new Frame(StreamType.STDOUT, payload.getBytes()));
        callback.close();
        assertThat(consumer.toUtf8String()).isEqualTo(payload);
    }

    @Test
    public void passStdoutSingleLine() throws IOException {
        String payload = "Test";
        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        ToStringConsumer consumer = new ToStringConsumer().withRemoveAnsiCodes(false);
        callback.addConsumer(OutputFrame.OutputType.STDOUT, consumer);
        callback.onNext(new Frame(StreamType.STDOUT, payload.getBytes()));
        callback.close();
        assertThat(consumer.toUtf8String()).isEqualTo(payload);
    }

    @Test
    public void passStdoutSingleLineWithNewline() throws IOException {
        String payload = "Test\n";
        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        ToStringConsumer consumer = new ToStringConsumer().withRemoveAnsiCodes(false);
        callback.addConsumer(OutputFrame.OutputType.STDOUT, consumer);
        callback.onNext(new Frame(StreamType.STDOUT, payload.getBytes()));
        callback.close();
        assertThat(consumer.toUtf8String()).isEqualTo(payload);
    }

    @Test
    public void passRawFrameWithoutColors() throws TimeoutException, IOException {
        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        WaitingConsumer waitConsumer = new WaitingConsumer();
        callback.addConsumer(OutputFrame.OutputType.STDOUT, waitConsumer);
        callback.onNext(new Frame(StreamType.RAW, FRAME_PAYLOAD.getBytes()));
        waitConsumer.waitUntil(
            frame -> frame.getType() == OutputFrame.OutputType.STDOUT && frame.getUtf8String().equals("Test2\n"),
            1,
            TimeUnit.SECONDS
        );
        waitConsumer.waitUntil(
            frame -> frame.getType() == OutputFrame.OutputType.STDOUT && frame.getUtf8String().equals("Тест1\n"),
            1,
            TimeUnit.SECONDS
        );
        Exception exception = null;
        try {
            waitConsumer.waitUntil(
                frame -> frame.getType() == OutputFrame.OutputType.STDOUT && frame.getUtf8String().equals("Test3"),
                1,
                TimeUnit.SECONDS
            );
        } catch (Exception e) {
            exception = e;
        }
        assertThat(exception instanceof TimeoutException).isTrue();
        callback.close();
        waitConsumer.waitUntil(
            frame -> frame.getType() == OutputFrame.OutputType.STDOUT && frame.getUtf8String().equals("Test3"),
            1,
            TimeUnit.SECONDS
        );
    }

    @Test
    public void passRawFrameWithColors() throws TimeoutException, IOException {
        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        WaitingConsumer waitConsumer = new WaitingConsumer().withRemoveAnsiCodes(false);
        callback.addConsumer(OutputFrame.OutputType.STDOUT, waitConsumer);
        callback.onNext(new Frame(StreamType.RAW, FRAME_PAYLOAD.getBytes()));
        waitConsumer.waitUntil(
            frame -> {
                return (
                    frame.getType() == OutputFrame.OutputType.STDOUT &&
                    frame.getUtf8String().equals("\u001B[1;33mTest2\u001B[0m\n")
                );
            },
            1,
            TimeUnit.SECONDS
        );
        waitConsumer.waitUntil(
            frame -> {
                return (
                    frame.getType() == OutputFrame.OutputType.STDOUT &&
                    frame.getUtf8String().equals("\u001B[0;32mТест1\u001B[0m\n")
                );
            },
            1,
            TimeUnit.SECONDS
        );
        Exception exception = null;
        try {
            waitConsumer.waitUntil(
                frame -> {
                    return (
                        frame.getType() == OutputFrame.OutputType.STDOUT &&
                        frame.getUtf8String().equals("\u001B[0;31mTest3\u001B[0m")
                    );
                },
                1,
                TimeUnit.SECONDS
            );
        } catch (Exception e) {
            exception = e;
        }
        assertThat(exception instanceof TimeoutException).isTrue();
        callback.close();
        waitConsumer.waitUntil(
            frame -> {
                return (
                    frame.getType() == OutputFrame.OutputType.STDOUT &&
                    frame.getUtf8String().equals("\u001B[0;31mTest3\u001B[0m")
                );
            },
            1,
            TimeUnit.SECONDS
        );
    }

    @Test
    public void reconstructBreakedUnicode() throws IOException {
        String payload = "Тест";
        byte[] payloadBytes = payload.getBytes(StandardCharsets.UTF_8);
        byte[] bytes1 = new byte[(int) (payloadBytes.length * 0.6)];
        byte[] bytes2 = new byte[payloadBytes.length - bytes1.length];
        System.arraycopy(payloadBytes, 0, bytes1, 0, bytes1.length);
        System.arraycopy(payloadBytes, bytes1.length, bytes2, 0, bytes2.length);
        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        ToStringConsumer consumer = new ToStringConsumer().withRemoveAnsiCodes(false);
        callback.addConsumer(OutputFrame.OutputType.STDOUT, consumer);
        callback.onNext(new Frame(StreamType.RAW, bytes1));
        callback.onNext(new Frame(StreamType.RAW, bytes2));
        callback.close();
        assertThat(consumer.toUtf8String()).isEqualTo(payload);
    }

    private static class BasicConsumer implements Consumer<OutputFrame> {

        private StringBuilder input = new StringBuilder();

        @Override
        public void accept(OutputFrame outputFrame) {
            input.append(outputFrame.getUtf8String());
        }

        @Override
        public String toString() {
            return input.toString();
        }
    }
}
