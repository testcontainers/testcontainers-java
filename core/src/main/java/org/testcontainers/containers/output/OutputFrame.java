package org.testcontainers.containers.output;

import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;

import java.nio.charset.StandardCharsets;

/**
 * Holds exactly one complete line of container output. Lines are split on newline characters (LF, CR LF).
 */
public class OutputFrame {

    public static final OutputFrame END = new OutputFrame(OutputType.END, null);

    private final OutputType type;

    private final byte[] bytes;

    public OutputFrame(final OutputType type, final byte[] bytes) {
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
        return (bytes == null) ? "" : new String(bytes, StandardCharsets.UTF_8);
    }

    public String getUtf8StringWithoutLineEnding() {
        if (bytes == null) {
            return "";
        }
        return new String(bytes, 0, bytes.length - determineLineEndingLength(bytes), StandardCharsets.UTF_8);
    }

    private static int determineLineEndingLength(final byte[] bytes) {
        if (bytes.length > 0) {
            switch (bytes[bytes.length - 1]) {
                case '\r':
                    return 1;
                case '\n':
                    return ((bytes.length > 1) && (bytes[bytes.length - 2] == '\r')) ? 2 : 1;
            }
        }
        return 0;
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
