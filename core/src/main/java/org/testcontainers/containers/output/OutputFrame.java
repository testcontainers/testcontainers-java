package org.testcontainers.containers.output;

import com.github.dockerjava.api.model.Frame;
import com.github.dockerjava.api.model.StreamType;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Holds exactly one complete line of container output. Lines are split on newline characters (LF, CR LF).
 */
public class OutputFrame {

    public static final OutputFrame END = new OutputFrame(OutputType.END, null, "");

    private final OutputType type;

    private final byte[] bytesWithoutLineEnding;

    private final String lineEnding;

    public OutputFrame(final OutputType type, final byte[] bytes) {
        this(type, bytes, "");
    }

    OutputFrame(final OutputType type, final byte[] bytesWithoutLineEnding, final String lineEnding) {
        this.type = type;
        this.bytesWithoutLineEnding = bytesWithoutLineEnding;
        this.lineEnding = lineEnding;
    }

    public OutputType getType() {
        return type;
    }

    public byte[] getBytesWithoutLineEnding() {
        return bytesWithoutLineEnding;
    }

    public String getUtf8StringWithoutLineEnding() {
        return (bytesWithoutLineEnding == null) ? "" : new String(bytesWithoutLineEnding, StandardCharsets.UTF_8);
    }

    public byte[] getBytes() {
        if (lineEnding.isEmpty()) {
            return bytesWithoutLineEnding;
        }
        final byte[] lineEndingBytes = lineEnding.getBytes(StandardCharsets.UTF_8);
        if (bytesWithoutLineEnding == null) {
            return lineEndingBytes;
        }
        final byte[] bytes = Arrays.copyOf(bytesWithoutLineEnding, bytesWithoutLineEnding.length + lineEndingBytes.length);
        System.arraycopy(lineEndingBytes, 0, bytes, bytesWithoutLineEnding.length, lineEndingBytes.length);
        return bytes;
    }

    public String getUtf8String() {
        if (bytesWithoutLineEnding == null) {
            return lineEnding;
        }
        return new String(bytesWithoutLineEnding, StandardCharsets.UTF_8) + lineEnding;
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
