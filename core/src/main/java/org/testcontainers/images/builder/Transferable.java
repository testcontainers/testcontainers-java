package org.testcontainers.images.builder;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.io.IOUtils;

import java.io.IOException;

public interface Transferable {

    int DEFAULT_FILE_MODE = 0100644;
    int DEFAULT_DIR_MODE = 040755;

    static Transferable of(byte[] bytes) {
        return of(bytes, DEFAULT_FILE_MODE);
    }

    static Transferable of(byte[] bytes, int fileMode) {
        return new Transferable() {
            @Override
            public long getSize() {
                return bytes.length;
            }

            @Override
            public byte[] getBytes() {
                return bytes;
            }

            @Override
            public int getFileMode() {
                return fileMode;
            }
        };
    }

    /**
     * Get file mode. Default is 0100644.
     *
     * @return file mode
     * @see Transferable#DEFAULT_FILE_MODE
     */
    default int getFileMode() {
        return DEFAULT_FILE_MODE;
    }

    /**
     * Size of an object.
     *
     * @return size in bytes
     */
    long getSize();

    /**
     * transfer content of this Transferable to the output stream. <b>Must not</b> close the stream.
     *
     * @param tarArchiveOutputStream stream to output
     * @param destination
     */
    default void transferTo(TarArchiveOutputStream tarArchiveOutputStream, final String destination) {
        TarArchiveEntry tarEntry = new TarArchiveEntry(destination);
        tarEntry.setSize(getSize());
        tarEntry.setMode(getFileMode());

        try {
            tarArchiveOutputStream.putArchiveEntry(tarEntry);
            IOUtils.write(getBytes(), tarArchiveOutputStream);
            tarArchiveOutputStream.closeArchiveEntry();
        } catch (IOException e) {
            throw new RuntimeException("Can't transfer " + getDescription(), e);
        }
    }

    default byte[] getBytes() {
        return new byte[0];
    }

    default String getDescription() {
        return "";
    }
}
