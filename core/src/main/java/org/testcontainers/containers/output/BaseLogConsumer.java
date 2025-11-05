package org.testcontainers.containers.output;

import java.util.function.Consumer;
import java.util.function.Supplier;

public abstract class BaseLogConsumer<SELF extends BaseLogConsumer<SELF>> extends BaseConsumer<BaseLogConsumer<SELF>> {
    protected boolean separateOutputStreams;
    protected String prefix = "";

    public BaseLogConsumer(boolean separateOutputStreams) {
        this.separateOutputStreams = separateOutputStreams;
    }

    public BaseLogConsumer<SELF> withPrefix(String prefix) {
        this.prefix = "[" + prefix + "] ";
        return this;
    }

    public BaseLogConsumer<SELF> withSeparateOutputStreams() {
        this.separateOutputStreams = true;
        return this;
    }

    protected void log(Consumer<Supplier<String>> loggerError, Consumer<Supplier<String>> loggerInfo, OutputFrame outputFrame) {
        final OutputFrame.OutputType outputType = outputFrame.getType();
        final String utf8String = outputFrame.getUtf8StringWithoutLineEnding();
        Supplier<String> seperateOSSupplier = () -> (prefix.isEmpty() ? "" : (prefix + ": ")) + utf8String;
        Supplier<String> jointOSSupplier = () -> prefix + outputType + ": " + utf8String;
        switch (outputType) {
            case END:
                break;
            case STDOUT:
                if (separateOutputStreams) {
                    loggerInfo.accept(seperateOSSupplier);
                } else {
                    loggerInfo.accept(jointOSSupplier);
                }
                break;
            case STDERR:
                if (separateOutputStreams) {
                    loggerError.accept(seperateOSSupplier);
                } else {
                    loggerInfo.accept(jointOSSupplier);
                }
                break;
            default:
                throw new IllegalArgumentException("Unexpected outputType " + outputType);
        }

    }
}
