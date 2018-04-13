package org.testcontainers.containers.output;

import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;
import static org.testcontainers.containers.output.OutputFrame.OutputType.STDOUT;

public class FrameConsumerResultCallbackTest {

    @Test
    public void passStderrFrameWithoutColors() {
        String payloadString = "line1\n\u001B[0;32mline2\n\u001B[0;31mline3\u001B[0m";
        String logString = "line1\nline2\nline3";

        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        ToStringConsumer consumer = new ToStringConsumer();
        callback.addConsumer(OutputFrame.OutputType.STDERR, consumer);
        callback.onNext(new Frame(StreamType.STDERR, payloadString.getBytes()));
        assertEquals(logString, consumer.toUtf8String());
    }

    @Test
    public void passStderrFrameWithColors() {
        String payloadString = "line1\n\u001B[0;32mline2\n\u001B[0;31mline3\u001B[0m";

        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        ToStringConsumer consumer = new ToStringConsumer().withRemoveAnsiCodes(false);
        callback.addConsumer(OutputFrame.OutputType.STDERR, consumer);
        callback.onNext(new Frame(StreamType.STDERR, payloadString.getBytes()));
        assertEquals(payloadString, consumer.toUtf8String());
    }

    @Test
    public void passStdoutFrameWithoutColors() {
        String payloadString = "line1\n\u001B[0;32mline2\n\u001B[0;31mline3\u001B[0m";
        String logString = "line1\nline2\nline3";

        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        ToStringConsumer consumer = new ToStringConsumer();
        callback.addConsumer(OutputFrame.OutputType.STDOUT, consumer);
        callback.onNext(new Frame(StreamType.STDOUT, payloadString.getBytes()));
        assertEquals(logString, consumer.toUtf8String());
    }

    @Test
    public void passStdoutFrameWithColors() {
        String payloadString = "line1\n\u001B[0;32mline2\n\u001B[0;31mline3\u001B[0m";

        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        ToStringConsumer consumer = new ToStringConsumer().withRemoveAnsiCodes(false);
        callback.addConsumer(OutputFrame.OutputType.STDOUT, consumer);
        callback.onNext(new Frame(StreamType.STDOUT, payloadString.getBytes()));
        assertEquals(payloadString, consumer.toUtf8String());
    }

    @Test
    public void passRawFrameWithoutColors() throws TimeoutException, IOException {
        String payloadString = "\u001B[0;32mline1\u001B[0m\n\u001B[1;33mline2\u001B[0m\n\u001B[0;31mline3\u001B[0m";

        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        WaitingConsumer waitConsumer = new WaitingConsumer();
        callback.addConsumer(OutputFrame.OutputType.STDOUT, waitConsumer);
        callback.onNext(new Frame(StreamType.RAW, payloadString.getBytes()));
        callback.close();
        waitConsumer.waitUntil(frame -> frame.getType() == STDOUT && frame.getUtf8String().equals("line3"), 1, TimeUnit.SECONDS);
        waitConsumer.waitUntil(frame -> frame.getType() == STDOUT && frame.getUtf8String().equals("line2"), 1, TimeUnit.SECONDS);
        waitConsumer.waitUntil(frame -> frame.getType() == STDOUT && frame.getUtf8String().equals("line1"), 1, TimeUnit.SECONDS);
    }

    @Test
    public void passRawFrameWithColors() throws TimeoutException, IOException {
        String payloadString = "\u001B[0;32mline1\u001B[0m\n\u001B[1;33mline2\u001B[0m\n\u001B[0;31mline3\u001B[0m";

        FrameConsumerResultCallback callback = new FrameConsumerResultCallback();
        WaitingConsumer waitConsumer = new WaitingConsumer().withRemoveAnsiCodes(false);
        callback.addConsumer(OutputFrame.OutputType.STDOUT, waitConsumer);
        callback.onNext(new Frame(StreamType.RAW, payloadString.getBytes()));
        callback.close();
        waitConsumer.waitUntil(frame -> frame.getType() == STDOUT && frame.getUtf8String().equals("\u001B[0;31mline3\u001B[0m"), 1, TimeUnit.SECONDS);
        waitConsumer.waitUntil(frame -> frame.getType() == STDOUT && frame.getUtf8String().equals("\u001B[1;33mline2\u001B[0m"), 1, TimeUnit.SECONDS);
        waitConsumer.waitUntil(frame -> frame.getType() == STDOUT && frame.getUtf8String().equals("\u001B[0;32mline1\u001B[0m"), 1, TimeUnit.SECONDS);
    }

}
