package org.testcontainers.containers.output;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.MDC;

/**
 * A consumer for container output that logs output to an SLF4J logger.
 */
public class Slf4jLogConsumer extends BaseConsumer<Slf4jLogConsumer> {
    private final Logger logger;
    private final Map<String, String> mdc = new HashMap<>();
    private boolean separateOutputStreams;
    private String prefix = "";

    public Slf4jLogConsumer(Logger logger) {
        this(logger, false);
    }

    public Slf4jLogConsumer(Logger logger, boolean separateOutputStreams) {
        this.logger = logger;
        this.separateOutputStreams = separateOutputStreams;
    }

    public Slf4jLogConsumer withPrefix(String prefix) {
        this.prefix = "[" + prefix + "] ";
        return this;
    }

    public Slf4jLogConsumer withMdc(String key, String value) {
        mdc.put(key, value);
        return this;
    }

    public Slf4jLogConsumer withMdc(Map<String, String> mdc) {
        this.mdc.putAll(mdc);
        return this;
    }

    public Slf4jLogConsumer withSeparateOutputStreams() {
        this.separateOutputStreams = true;
        return this;
    }

    @Override
    public void accept(OutputFrame outputFrame) {
        OutputFrame.OutputType outputType = outputFrame.getType();

        String utf8String = outputFrame.getUtf8String();
        utf8String = utf8String.replaceAll(FrameConsumerResultCallback.LINE_BREAK_AT_END_REGEX, "");

        Map<String, String> originalMdc = MDC.getCopyOfContextMap();
        MDC.setContextMap(mdc);
        try {
            switch (outputType) {
                case END:
                    break;
                case STDOUT:
                    if (separateOutputStreams) {
                        logger.info("{}{}", prefix.isEmpty() ? "" : (prefix + ": "), utf8String);
                    } else {
                        logger.info("{}{}: {}", prefix, outputType, utf8String);
                    }
                    break;
                case STDERR:
                    if (separateOutputStreams) {
                        logger.error("{}{}", prefix.isEmpty() ? "" : (prefix + ": "), utf8String);
                    } else {
                        logger.info("{}{}: {}", prefix, outputType, utf8String);
                    }
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected outputType " + outputType);
            }
        } finally {
            if (originalMdc == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(originalMdc);
            }
        }
    }
}
