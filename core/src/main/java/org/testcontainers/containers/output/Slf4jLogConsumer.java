package org.testcontainers.containers.output;

import org.slf4j.Logger;

import java.util.function.Consumer;
import java.util.regex.Pattern;

/**
 * A consumer for container output that logs output to an SLF4J logger.
 */
public class Slf4jLogConsumer implements Consumer<OutputFrame> {
    private final Logger logger;
    private String prefix = "";

    private static final Pattern ANSI_CODE_PATTERN = Pattern.compile("\\[\\d[ABCD]");

    public Slf4jLogConsumer(Logger logger) {
        this.logger = logger;
    }

    public Slf4jLogConsumer withPrefix(String prefix) {
        this.prefix = "[" + prefix + "] ";
        return this;
    }

    @Override
    public void accept(OutputFrame outputFrame) {
        if (outputFrame != null) {
            String utf8String = outputFrame.getUtf8String();

            if (utf8String != null) {
                OutputFrame.OutputType outputType = outputFrame.getType();
                String message = utf8String.trim();

                if (ANSI_CODE_PATTERN.matcher(message).matches()) {
                    return;
                }

                switch (outputType) {
                    case END:
                        break;
                    case STDOUT:
                    case STDERR:
                        logger.info("{}{}: {}", prefix, outputType, message);
                        break;
                    default:
                        throw new IllegalArgumentException("Unexpected outputType " + outputType);
                }
            }
        }
    }
}
