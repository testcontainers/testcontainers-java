package org.testcontainers.images.builder.traits;

import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
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
            @SneakyThrows(IOException.class)
            public void transferTo(OutputStream outputStream) {
                IOUtils.write(bytes, outputStream);
            }

        });
    }
}
