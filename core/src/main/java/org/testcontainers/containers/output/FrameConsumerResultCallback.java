package org.testcontainers.containers.output;


import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.core.async.ResultCallbackTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

/**
 * This class can be used as a generic callback for docker-java commands that produce Frames.
 */
public class FrameConsumerResultCallback extends ResultCallbackTemplate<FrameConsumerResultCallback, Frame> {

    private final static Logger LOGGER = LoggerFactory.getLogger(FrameConsumerResultCallback.class);

    private Map<OutputFrame.OutputType, Consumer<OutputFrame>> consumers;

    private CountDownLatch completionLatch = new CountDownLatch(1);

    public FrameConsumerResultCallback() {
        consumers = new HashMap<>();
    }

    /**
     * Set this callback to use the specified consumer for the given output type.
     * The same consumer can be configured for more than one output type.
     * @param outputType the output type to configure
     * @param consumer the consumer to use for that output type
     */
    public void addConsumer(OutputFrame.OutputType outputType, Consumer<OutputFrame> consumer) {
        consumers.put(outputType, consumer);
    }

    @Override
    public void onNext(Frame frame) {
        if (frame != null) {
            OutputFrame outputFrame = OutputFrame.forFrame(frame);
            if (outputFrame != null) {
                Consumer<OutputFrame> consumer = consumers.get(outputFrame.getType());
                if (consumer == null) {
                    LOGGER.error("got frame with type " + frame.getStreamType() + ", for which no handler is configured");
                } else {
                    consumer.accept(outputFrame);
                }
            }
        }
    }

    @Override
    public void onError(Throwable throwable) {
        // Sink any errors
        try {
            close();
        } catch (IOException ignored) { }
    }

    @Override
    public void close() throws IOException {
        // send an END frame to every consumer... but only once per consumer.
        for (Consumer<OutputFrame> consumer : new HashSet<>(consumers.values())) {
            consumer.accept(OutputFrame.END);
        }
        super.close();

        completionLatch.countDown();
    }

    /**
     * @return a {@link CountDownLatch} that may be used to wait until {@link #close()} has been called.
     */
    public CountDownLatch getCompletionLatch() {
        return completionLatch;
    }
}
