package org.testcontainers.images.builder.traits;

import org.testcontainers.images.builder.Transferable;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * BuildContextBuilder's trait for NIO-based (Files and Paths) manipulations.
 *
 */
public interface FilesTrait<SELF extends FilesTrait<SELF> & BuildContextBuilderTrait<SELF>> {

    default SELF withFileFromFile(String path, File file) {
        return withFileFromPath(path, file.toPath());
    }

    default SELF withFileFromPath(String path, Path filePath) {
        return ((SELF) this).withFileFromTransferable(path, new Transferable() {

            @Override
            public long getSize() {
                try {
                    return Files.size(filePath);
                } catch (IOException e) {
                    throw new RuntimeException("Can't get size from " + filePath, e);
                }
            }

            @Override
            public int getFileMode() {
                return DEFAULT_FILE_MODE | (Files.isExecutable(filePath) ? 0755 : 0);
            }

            @Override
            public void transferTo(OutputStream outputStream) {
                try {
                    Files.copy(filePath, outputStream);
                } catch (IOException e) {
                    throw new RuntimeException("Can't transfer file " + filePath, e);
                }
            }

        });
    }
}
