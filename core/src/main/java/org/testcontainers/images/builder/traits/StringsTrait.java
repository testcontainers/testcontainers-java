package org.testcontainers.images.builder.traits;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.testcontainers.images.builder.Transferable;

import java.io.IOException;
import java.io.OutputStream;

/**
 * BuildContextBuilder's trait for String-based manipulations.
 *
 */
public interface StringsTrait<SELF extends StringsTrait<SELF> & BuildContextBuilderTrait<SELF>> {

    default SELF withFileFromString(String path, String content) {
        return ((SELF) this).withFileFromTransferable(path, new Transferable() {

            byte[] bytes = content.getBytes();

            @Override
            public long getSize() {
                return bytes.length;
            }

            @Override
            public void transferTo(OutputStream outputStream) {
                try {
                    IOUtils.write(bytes, outputStream);
                } catch (IOException e) {
                    throw new RuntimeException("Can't transfer string " + StringUtils.abbreviate(content, 100), e);
                }
            }

        });
    }
}
