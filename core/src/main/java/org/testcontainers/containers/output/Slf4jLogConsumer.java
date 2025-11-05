package org.testcontainers.containers.output;

import org.slf4j.Logger;
import org.slf4j.MDC;

import java.util.HashMap;
import java.util.Map;

/**
 * A consumer for container output that logs output to an SLF4J logger.
 */
public class Slf4jLogConsumer extends BaseLogConsumer<Slf4jLogConsumer> {

    private final Logger logger;

    private final Map<String, String> mdc = new HashMap<>();

    public Slf4jLogConsumer(Logger logger) {
        this(logger, false);
    }

    public Slf4jLogConsumer(Logger logger, boolean separateOutputStreams) {
        super(separateOutputStreams);
        this.logger = logger;
    }

    public Slf4jLogConsumer withMdc(String key, String value) {
        mdc.put(key, value);
        return this;
    }

    public Slf4jLogConsumer withMdc(Map<String, String> mdc) {
        this.mdc.putAll(mdc);
        return this;
    }

    @Override
    public void accept(OutputFrame outputFrame) {
        final Map<String, String> originalMdc = MDC.getCopyOfContextMap();
        MDC.setContextMap(mdc);
        try {
            log((s) -> {
                if (logger.isErrorEnabled()) {
                    logger.error(s.get());
                }
            }, (s) -> {
                if (logger.isInfoEnabled()) {
                    logger.info(s.get());
                }
            }, outputFrame);
        } finally {
            if (originalMdc == null) {
                MDC.clear();
            } else {
                MDC.setContextMap(originalMdc);
            }
        }
    }
}
