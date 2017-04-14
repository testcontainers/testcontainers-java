package org.testcontainers.images.builder.traits;

import org.testcontainers.utility.MountableFile;

import java.io.File;
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
        final MountableFile mountableFile = MountableFile.forHostPath(filePath);
        return ((SELF) this).withFileFromTransferable(path, mountableFile);
    }
}
