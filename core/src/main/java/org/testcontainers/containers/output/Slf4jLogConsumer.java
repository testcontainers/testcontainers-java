package org.testcontainers.containers.output;

import org.slf4j.Logger;

import java.util.function.Consumer;

/**
 * A consumer for container output that logs output to an SLF4J logger.
 */
public class Slf4jLogConsumer implements Consumer<OutputFrame> {
    private final Logger logger;

    public Slf4jLogConsumer(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void accept(OutputFrame outputFrame) {
        logger.info("{}: {}", outputFrame.getType(), outputFrame.getUtf8String());
    }
}
