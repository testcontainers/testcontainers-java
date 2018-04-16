package org.testcontainers.containers.output;

import com.google.common.base.Charsets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Created by rnorth on 26/03/2016.
 */
public class ToStringConsumer extends BaseConsumer<ToStringConsumer> {
    private static final byte[] NEW_LINE = "\n".getBytes();

    private boolean firstLine = true;
    private ByteArrayOutputStream stringBuffer = new ByteArrayOutputStream();

    @Override
    public void accept(OutputFrame outputFrame) {
        try {
            if (outputFrame.getBytes() != null) {
                if (!firstLine) {
                    stringBuffer.write(NEW_LINE);
                }
                stringBuffer.write(outputFrame.getBytes());
                stringBuffer.flush();
                firstLine = false;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String toUtf8String() {
        byte[] bytes = stringBuffer.toByteArray();
        return new String(bytes, Charsets.UTF_8);
    }

    public String toString(Charset charset) {
        byte[] bytes = stringBuffer.toByteArray();
        return new String(bytes, charset);
    }
}
