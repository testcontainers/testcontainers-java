package org.testcontainers.containers.output;

import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;
import com.google.common.base.Charsets;

/**
 * Holds exactly one complete line of container output without any newline characters.
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
            return "";
        }

        return new String(bytes, Charsets.UTF_8);
    }

    public enum OutputType {
        STDOUT,
        STDERR,
        END;

        public static OutputType forStreamType(StreamType streamType) {
            switch (streamType) {
                case RAW:
                case STDOUT:
                    return STDOUT;
                case STDERR:
                    return STDERR;
                default:
                    return null;
            }
        }
    }

    public static OutputFrame forFrame(Frame frame) {
        final OutputType outputType = OutputType.forStreamType(frame.getStreamType());
        if (outputType == null) {
            return null;
        }
        return new OutputFrame(outputType, frame.getPayload());
    }
}
