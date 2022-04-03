package org.testcontainers.containers.output;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import org.testcontainers.containers.output.OutputFrame.OutputType;

/**
 * Base class for OutputFrame log consumers, OutputTypes can be configured (all by default).<br>
 * Logging can be disabled at runtime by calling {@link #setSkip(boolean)}.
 *
 * @param <TYPE> Logger type, i.e. org.slf4j.Logger
 */
public abstract class LogConsumer<SELF extends LogConsumer<SELF, TYPE>, TYPE> extends BaseConsumer<LogConsumer<SELF, TYPE>> {
    @Getter
    @Setter
    protected TYPE logger;
    protected boolean skip;

    /**
     * @param logger Logger to write OutputFrames to (all OutputTypes)
     */
    public LogConsumer(@NonNull TYPE logger) {
        this(logger, OutputType.values());
    }

    /**
     * @param logger Logger to write OutputFrames to
     * @param types  OutputTypes which should be logged (empty logs all)
     */
    public LogConsumer(@NonNull TYPE logger, OutputType... types) {
        super(types);
        this.logger = logger;
    }

    /**
     * @return {@code true} if logging is disabled
     */
    public boolean isSkip() {
        return skip;
    }

    /**
     * @param skip Do not log anymore
     */
    @SuppressWarnings("unchecked")
    public SELF setSkip(boolean skip) {
        this.skip = skip;
        return (SELF) this;
    }

    @Override
    public void accept(OutputFrame outputFrame) {
        if (skip) {
            return;
        }
        log(outputFrame);
    }

    /**
     * Log the outputFrame
     */
    public abstract void log(OutputFrame outputFrame);
}
