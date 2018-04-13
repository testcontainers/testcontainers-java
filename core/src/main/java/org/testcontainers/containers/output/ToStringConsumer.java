package org.testcontainers.containers.output;

import com.google.common.base.Charsets;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

/**
 * Created by rnorth on 26/03/2016.
 */
public class ToStringConsumer extends BaseConsumer<ToStringConsumer> {
    private static final byte[] ENTER_BYTES = "\n".getBytes();

    private ByteArrayOutputStream stringBuffer = new ByteArrayOutputStream();

    @Override
    public void process(OutputFrame outputFrame) {
        try {
            if (outputFrame.getBytes() != null) {
                stringBuffer.write(outputFrame.getBytes());
                stringBuffer.write(ENTER_BYTES);
                stringBuffer.flush();
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
