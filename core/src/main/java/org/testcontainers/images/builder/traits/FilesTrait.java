package org.testcontainers.images.builder.traits;

import lombok.SneakyThrows;
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
            @SneakyThrows(IOException.class)
            public long getSize() {
                return Files.size(filePath);
            }

            @Override
            public int getFileMode() {
                return DEFAULT_FILE_MODE | (Files.isExecutable(filePath) ? 0755 : 0);
            }

            @Override
            @SneakyThrows(IOException.class)
            public void transferTo(OutputStream outputStream) {
                Files.copy(filePath, outputStream);
            }

        });
    }
}
