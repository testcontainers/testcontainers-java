package org.testcontainers.containers.output;

import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.model.Frame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * This class can be used as a generic callback for docker-java commands that produce Frames.
 */
public class FrameConsumerResultCallback extends ResultCallbackTemplate<FrameConsumerResultCallback, Frame> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrameConsumerResultCallback.class);

    private final Map<OutputFrame.OutputType, LineConsumer> consumers = new HashMap<>();

    private final CountDownLatch completionLatch = new CountDownLatch(1);

    /**
     * Set this callback to use the specified consumer for the given output type.
     * The same consumer can be configured for more than one output type.
     * @param outputType the output type to configure
     * @param consumer the consumer to use for that output type
     */
    public void addConsumer(OutputFrame.OutputType outputType, Consumer<OutputFrame> consumer) {
        consumers.put(outputType, new LineConsumer(outputType, consumer));
    }

    @Override
    public void onNext(Frame frame) {
        if (frame != null) {
            final OutputFrame.OutputType type = OutputFrame.OutputType.forStreamType(frame.getStreamType());
            if (type != null) {
                final LineConsumer consumer = consumers.get(type);
                if (consumer == null) {
                    LOGGER.error("got frame with type {}, for which no handler is configured", frame.getStreamType());
                } else if (frame.getPayload() != null) {
                    consumer.processFrame(frame.getPayload());
                }
            }
        }
    }

    @Override
    public void onError(Throwable throwable) {
        // Sink any errors
        try {
            close();
        } catch (IOException ignored) {}
    }

    @Override
    public void close() throws IOException {
        if (completionLatch.getCount() == 0) {
            return;
        }

        consumers.values().forEach(LineConsumer::processBuffer);
        consumers.values().forEach(LineConsumer::end);
        super.close();

        completionLatch.countDown();
    }

    /**
     * @return a {@link CountDownLatch} that may be used to wait until {@link #close()} has been called.
     */
    public CountDownLatch getCompletionLatch() {
        return completionLatch;
    }

    private static class LineConsumer {

        private static final Pattern ANSI_COLOR_PATTERN = Pattern.compile("\u001B\\[[0-9;]+m");

        private final OutputFrame.OutputType type;

        private final Consumer<OutputFrame> consumer;

        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        private boolean lastCR = false;

        LineConsumer(final OutputFrame.OutputType type, final Consumer<OutputFrame> consumer) {
            this.type = type;
            this.consumer = consumer;
        }

        void processFrame(final byte[] b) {
            int start = 0;
            int i = 0;
            while (i < b.length) {
                switch (b[i]) {
                    case '\n':
                        buffer.write(b, start, i + 1 - start);
                        start = i + 1;
                        consume();
                        lastCR = false;
                        break;
                    case '\r':
                        if (lastCR) {
                            consume();
                        }
                        buffer.write(b, start, i + 1 - start);
                        start = i + 1;
                        lastCR = true;
                        break;
                    default:
                        if (lastCR) {
                            consume();
                        }
                        lastCR = false;
                }
                i++;
            }
            buffer.write(b, start, b.length - start);
        }

        void processBuffer() {
            if (buffer.size() > 0) {
                consume();
            }
        }

        void end() {
            consumer.accept(OutputFrame.END);
        }

        private void consume() {
            final String string = new String(buffer.toByteArray(), StandardCharsets.UTF_8);
            final byte[] bytes = processAnsiColorCodes(string).getBytes(StandardCharsets.UTF_8);
            consumer.accept(new OutputFrame(type, bytes));
            buffer.reset();
        }

        private String processAnsiColorCodes(final String utf8String) {
            if (!(consumer instanceof BaseConsumer) || ((BaseConsumer<?>) consumer).isRemoveColorCodes()) {
                return ANSI_COLOR_PATTERN.matcher(utf8String).replaceAll("");
            }
            return utf8String;
        }
    }
}
