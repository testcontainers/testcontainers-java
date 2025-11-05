package org.testcontainers.containers.output;

import java.util.logging.Logger;

/**
 * A consumer for container output that logs output to a {@link java.util.logging.Logger}.
 */
public class JulLogConsumer extends BaseLogConsumer<JulLogConsumer> {

    private final Logger logger;

    public JulLogConsumer(Logger logger) {
        this(logger, false);
    }

    public JulLogConsumer(Logger logger, boolean separateOutputStreams) {
        super(separateOutputStreams);
        this.logger = logger;
    }

    @Override
    public void accept(OutputFrame outputFrame) {
        log(logger::severe, logger::info, outputFrame);
    }
}
