package org.testcontainers.images.builder.traits;

import org.testcontainers.utility.MountableFile;

import java.nio.file.Paths;

/**
 * BuildContextBuilder's trait for classpath-based resources.
 *
 */
public interface ClasspathTrait<SELF extends ClasspathTrait<SELF> & BuildContextBuilderTrait<SELF> & FilesTrait<SELF>> {

    default SELF withFileFromClasspath(String path, String resourcePath) {
        final MountableFile mountableFile = MountableFile.forClasspathResource(resourcePath);

        return ((SELF) this).withFileFromPath(path, Paths.get(mountableFile.getResolvedPath()));
    }
}
