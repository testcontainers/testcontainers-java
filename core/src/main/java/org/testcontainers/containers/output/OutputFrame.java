package org.testcontainers.containers.output;

import com.google.common.base.Charsets;

/**
 * Holds a frame of container output (usually one line, possibly more)
 */
public class OutputFrame {

    public static final OutputFrame END = new OutputFrame(OutputType.END, null);

    private final OutputType type;
    private final byte[] bytes;

    public OutputFrame(OutputType type, byte[] bytes) {
        this.type = type;
        this.bytes = bytes;
    }

    public OutputType getType() {
        return type;
    }

    public byte[] getBytes() {
        return bytes;
    }

    public String getUtf8String() {

        if (bytes == null) {
            return null;
        }

        return new String(bytes, Charsets.UTF_8);
    }

    public enum OutputType {
        STDOUT, STDERR, END
    }
}
