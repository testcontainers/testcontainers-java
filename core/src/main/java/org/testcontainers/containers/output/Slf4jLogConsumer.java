package org.testcontainers.containers.output;

import org.slf4j.Logger;

/**
 * A consumer for container output that logs output to an SLF4J logger.
 */
public class Slf4jLogConsumer extends BaseConsumer<Slf4jLogConsumer> {
    private final Logger logger;
    private String prefix = "";

    public Slf4jLogConsumer(Logger logger) {
        this.logger = logger;
    }

    public Slf4jLogConsumer withPrefix(String prefix) {
        this.prefix = "[" + prefix + "] ";
        return this;
    }

    @Override
    public void accept(OutputFrame outputFrame) {
        OutputFrame.OutputType outputType = outputFrame.getType();

        String utf8String = outputFrame.getUtf8String();
        utf8String = utf8String.replaceAll(FrameConsumerResultCallback.LINE_BREAK_AT_END_REGEX, "");
        switch (outputType) {
            case END:
                break;
            case STDOUT:
            case STDERR:
                logger.info("{}{}: {}", prefix, outputType, utf8String);
                break;
            default:
                throw new IllegalArgumentException("Unexpected outputType " + outputType);
        }
    }
}
