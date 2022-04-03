package org.testcontainers.containers.output;

import lombok.NonNull;
import org.apache.commons.lang.StringUtils;
import org.testcontainers.containers.output.OutputFrame.OutputType;

import java.util.logging.Logger;

/**
 * A consumer for container output that logs with java.util.logging.
 * stdout is mapped to info(), stderr to severe().
 */
public class JavaUtilLogConsumer extends LogConsumer<JavaUtilLogConsumer, Logger> {
    /**
     * @param logger stdout is mapped to info(), stderr to severe().
     */
    public JavaUtilLogConsumer(@NonNull Logger logger) {
        super(logger);
    }

    /**
     * @param logger stdout is mapped to info(), stderr to severe().
     * @param types  OutputTypes which should be logged (empty logs all)
     */
    public JavaUtilLogConsumer(@NonNull Logger logger, @NonNull OutputType... types) {
        super(logger, types);
    }

    @Override
    public void log(OutputFrame outputFrame) {
        String trimmed = StringUtils.stripEnd(outputFrame.getUtf8String(), null);
        switch (outputFrame.getType()) {
            case STDERR:
                getLogger().severe(trimmed);
                break;
            case STDOUT:
                getLogger().info(trimmed);
        }
    }
}
