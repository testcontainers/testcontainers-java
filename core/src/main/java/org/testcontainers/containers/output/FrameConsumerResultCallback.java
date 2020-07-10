package org.testcontainers.containers.output;


import com.github.dockerjava.api.async.ResultCallbackTemplate;
import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * This class can be used as a generic callback for docker-java commands that produce Frames.
 */
public class FrameConsumerResultCallback extends ResultCallbackTemplate<FrameConsumerResultCallback, Frame> {

    private static final Logger LOGGER = LoggerFactory.getLogger(FrameConsumerResultCallback.class);

    private static final byte[] EMPTY_LINE = new byte[0];

    private static final Pattern ANSI_COLOR_PATTERN = Pattern.compile("\u001B\\[[0-9;]+m");

    private static final String LINE_BREAK_REGEX = "((\\r?\\n)|(\\r))";

    static final String LINE_BREAK_AT_END_REGEX = LINE_BREAK_REGEX + "$";

    private Map<OutputFrame.OutputType, Consumer<OutputFrame>> consumers;

    private CountDownLatch completionLatch = new CountDownLatch(1);

    private StringBuilder logString = new StringBuilder();

    private OutputFrame brokenFrame;

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
                    LOGGER.error("got frame with type {}, for which no handler is configured", frame.getStreamType());
                } else if (outputFrame.getBytes() != null && outputFrame.getBytes().length > 0) {
                    if (frame.getStreamType() == StreamType.RAW) {
                        processRawFrame(outputFrame, consumer);
                    } else {
                        processOtherFrame(outputFrame, consumer);
                    }
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
        OutputFrame lastLine = null;

        if (logString.length() > 0) {
            lastLine = new OutputFrame(OutputFrame.OutputType.STDOUT, logString.toString().getBytes());
        }

        // send an END frame to every consumer... but only once per consumer.
        for (Consumer<OutputFrame> consumer : new HashSet<>(consumers.values())) {
            if (lastLine != null) {
                consumer.accept(lastLine);
            }
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

    private synchronized void processRawFrame(OutputFrame outputFrame, Consumer<OutputFrame> consumer) {
        String utf8String = outputFrame.getUtf8String();
        byte[] bytes = outputFrame.getBytes();

        // Merging the strings by bytes to solve the problem breaking non-latin unicode symbols.
        if (brokenFrame != null) {
            bytes = merge(brokenFrame.getBytes(), bytes);
            utf8String = new String(bytes);
            brokenFrame = null;
        }
        // Logger chunks can break the string in middle of multibyte unicode character.
        // Backup the bytes to reconstruct proper char sequence with bytes from next frame.
        int lastCharacterType = Character.getType(utf8String.charAt(utf8String.length() - 1));
        if (lastCharacterType == Character.OTHER_SYMBOL) {
            brokenFrame = new OutputFrame(outputFrame.getType(), bytes);
            return;
        }

        utf8String = processAnsiColorCodes(utf8String, consumer);
        normalizeLogLines(utf8String, consumer);
    }

    private synchronized void processOtherFrame(OutputFrame outputFrame, Consumer<OutputFrame> consumer) {
        String utf8String = outputFrame.getUtf8String();

        utf8String = processAnsiColorCodes(utf8String, consumer);
        consumer.accept(new OutputFrame(outputFrame.getType(), utf8String.getBytes()));
    }

    private void normalizeLogLines(String utf8String, Consumer<OutputFrame> consumer) {
        // Reformat strings to normalize new lines.
        List<String> lines = new ArrayList<>(Arrays.asList(utf8String.split(LINE_BREAK_REGEX)));
        if (lines.isEmpty()) {
            consumer.accept(new OutputFrame(OutputFrame.OutputType.STDOUT, EMPTY_LINE));
            return;
        }
        if (utf8String.startsWith("\n") || utf8String.startsWith("\r")) {
            lines.add(0, "");
        }
        if (utf8String.endsWith("\n") || utf8String.endsWith("\r")) {
            lines.add("");
        }
        for (int i = 0; i < lines.size() - 1; i++) {
            String line = lines.get(i);
            if (i == 0 && logString.length() > 0) {
                line = logString.toString() + line;
                logString.setLength(0);
            }
            consumer.accept(new OutputFrame(OutputFrame.OutputType.STDOUT, line.getBytes()));
        }
        logString.append(lines.get(lines.size() - 1));
    }

    private String processAnsiColorCodes(String utf8String, Consumer<OutputFrame> consumer) {
        if (!(consumer instanceof BaseConsumer) || ((BaseConsumer) consumer).isRemoveColorCodes()) {
            return ANSI_COLOR_PATTERN.matcher(utf8String).replaceAll("");
        }
        return utf8String;
    }


    private byte[] merge(byte[] str1, byte[] str2) {
        byte[] mergedString = new byte[str1.length + str2.length];
        System.arraycopy(str1, 0, mergedString, 0, str1.length);
        System.arraycopy(str2, 0, mergedString, str1.length, str2.length);
        return mergedString;
    }
}
