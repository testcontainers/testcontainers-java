package org.testcontainers.images.builder;

import java.io.OutputStream;

public interface Transferable {

    int DEFAULT_FILE_MODE = 0100644;

    /**
     * Get file mode. Default is 0100644.
     * @see Transferable#DEFAULT_FILE_MODE
     *
     * @return file mode
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
     * @param outputStream stream to output
     */
    void transferTo(OutputStream outputStream);
}
