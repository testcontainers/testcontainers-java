package org.testcontainers.containers.output;

import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.testcontainers.containers.output.OutputFrame.OutputType;

public class FrameConsumerResultCallbackTest {
    private static final String FRAME_PAYLOAD = "\u001B[0;32mТест1\u001B[0m\n\u001B[1;33mTest2\u001B[0m\n\u001B[0;31mTest3\u001B[0m";
    private static final String LOG_RESULT = "Тест1\nTest2\nTest3";

    @Test
    public void passStderrFrameWithoutColors() {
        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        ToStringConsumer consumer = new ToStringConsumer();
        callback.addConsumer(OutputType.STDERR, consumer);
        callback.onNext(new Frame(StreamType.STDERR, FRAME_PAYLOAD.getBytes()));
        assertEquals(LOG_RESULT, consumer.toUtf8String());
    }

    @Test
    public void passStderrFrameWithColors() {
        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        ToStringConsumer consumer = new ToStringConsumer().withRemoveAnsiCodes(false);
        callback.addConsumer(OutputType.STDERR, consumer);
        callback.onNext(new Frame(StreamType.STDERR, FRAME_PAYLOAD.getBytes()));
        assertEquals(FRAME_PAYLOAD, consumer.toUtf8String());
    }

    @Test
    public void passStdoutFrameWithoutColors() {
        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        ToStringConsumer consumer = new ToStringConsumer();
        callback.addConsumer(OutputType.STDOUT, consumer);
        callback.onNext(new Frame(StreamType.STDOUT, FRAME_PAYLOAD.getBytes()));
        assertEquals(LOG_RESULT, consumer.toUtf8String());
    }

    @Test
    public void passStdoutFrameWithColors() {
        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        ToStringConsumer consumer = new ToStringConsumer().withRemoveAnsiCodes(false);
        callback.addConsumer(OutputType.STDOUT, consumer);
        callback.onNext(new Frame(StreamType.STDOUT, FRAME_PAYLOAD.getBytes()));
        assertEquals(FRAME_PAYLOAD, consumer.toUtf8String());
    }

    @Test
    public void basicConsumer() {
        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        BasicConsumer consumer = new BasicConsumer();
        callback.addConsumer(OutputType.STDOUT, consumer);
        callback.onNext(new Frame(StreamType.STDOUT, FRAME_PAYLOAD.getBytes()));
        assertEquals(LOG_RESULT, consumer.toString());
    }

    @Test
    public void passStdoutNull() {
        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        ToStringConsumer consumer = new ToStringConsumer().withRemoveAnsiCodes(false);
        callback.addConsumer(OutputType.STDOUT, consumer);
        callback.onNext(new Frame(StreamType.STDOUT, null));
        assertEquals("", consumer.toUtf8String());
    }

    @Test
    public void passStdoutEmptyLine() {
        String payload = "";
        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        ToStringConsumer consumer = new ToStringConsumer().withRemoveAnsiCodes(false);
        callback.addConsumer(OutputType.STDOUT, consumer);
        callback.onNext(new Frame(StreamType.STDOUT, payload.getBytes()));
        assertEquals(payload, consumer.toUtf8String());
    }

    @Test
    public void passStdoutSingleLine() {
        String payload = "Test";
        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        ToStringConsumer consumer = new ToStringConsumer().withRemoveAnsiCodes(false);
        callback.addConsumer(OutputType.STDOUT, consumer);
        callback.onNext(new Frame(StreamType.STDOUT, payload.getBytes()));
        assertEquals(payload, consumer.toUtf8String());
    }

    @Test
    public void passStdoutSingleLineWithNewline() {
        String payload = "Test\n";
        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        ToStringConsumer consumer = new ToStringConsumer().withRemoveAnsiCodes(false);
        callback.addConsumer(OutputType.STDOUT, consumer);
        callback.onNext(new Frame(StreamType.STDOUT, payload.getBytes()));
        assertEquals(payload, consumer.toUtf8String());
    }

    @Test
    public void passRawFrameWithoutColors() throws TimeoutException, IOException {
        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        WaitingConsumer waitConsumer = new WaitingConsumer();
        callback.addConsumer(OutputType.STDOUT, waitConsumer);
        callback.onNext(new Frame(StreamType.RAW, FRAME_PAYLOAD.getBytes()));
        waitConsumer.waitUntil(frame -> frame.getType() == OutputType.STDOUT && frame.getUtf8String().equals("Test2"), 1, TimeUnit.SECONDS);
        waitConsumer.waitUntil(frame -> frame.getType() == OutputType.STDOUT && frame.getUtf8String().equals("Тест1"), 1, TimeUnit.SECONDS);
        Exception exception = null;
        try {
            waitConsumer.waitUntil(frame -> frame.getType() == OutputType.STDOUT && frame.getUtf8String().equals("Test3"), 1, TimeUnit.SECONDS);
        } catch (Exception e) {
            exception = e;
        }
        assertTrue(exception instanceof TimeoutException);
        callback.close();
        waitConsumer.waitUntil(frame -> frame.getType() == OutputType.STDOUT && frame.getUtf8String().equals("Test3"), 1, TimeUnit.SECONDS);
    }

    @Test
    public void passRawFrameWithColors() throws TimeoutException, IOException {
        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        WaitingConsumer waitConsumer = new WaitingConsumer().withRemoveAnsiCodes(false);
        callback.addConsumer(OutputType.STDOUT, waitConsumer);
        callback.onNext(new Frame(StreamType.RAW, FRAME_PAYLOAD.getBytes()));
        waitConsumer.waitUntil(frame -> frame.getType() == OutputType.STDOUT && frame.getUtf8String().equals("\u001B[1;33mTest2\u001B[0m"), 1, TimeUnit.SECONDS);
        waitConsumer.waitUntil(frame -> frame.getType() == OutputType.STDOUT && frame.getUtf8String().equals("\u001B[0;32mТест1\u001B[0m"), 1, TimeUnit.SECONDS);
        Exception exception = null;
        try {
            waitConsumer.waitUntil(frame -> frame.getType() == OutputType.STDOUT && frame.getUtf8String().equals("\u001B[0;31mTest3\u001B[0m"), 1, TimeUnit.SECONDS);
        } catch (Exception e) {
            exception = e;
        }
        assertTrue(exception instanceof TimeoutException);
        callback.close();
        waitConsumer.waitUntil(frame -> frame.getType() == OutputType.STDOUT && frame.getUtf8String().equals("\u001B[0;31mTest3\u001B[0m"), 1, TimeUnit.SECONDS);
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
        callback.addConsumer(OutputType.STDOUT, consumer);
        callback.onNext(new Frame(StreamType.RAW, bytes1));
        callback.onNext(new Frame(StreamType.RAW, bytes2));
        callback.close();
        assertEquals(payload, consumer.toUtf8String());
    }

    private static class BasicConsumer implements Consumer<OutputFrame> {
        private boolean firstLine = true;
        private StringBuilder input = new StringBuilder();

        @Override
        public void accept(OutputFrame outputFrame) {
            if (!firstLine) {
                input.append('\n');
            }
            firstLine = false;
            input.append(outputFrame.getUtf8String());
        }

        @Override
        public String toString() {
            return input.toString();
        }
    }
}
