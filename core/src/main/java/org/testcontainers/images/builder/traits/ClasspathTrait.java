package org.testcontainers.images.builder.traits;

import java.io.File;
import java.net.URL;
import java.nio.file.Paths;

/**
 * BuildContextBuilder's trait for classpath-based resources.
 *
 */
public interface ClasspathTrait<SELF extends ClasspathTrait<SELF> & BuildContextBuilderTrait<SELF> & FilesTrait<SELF>> {

    default SELF withFileFromClasspath(String path, String resourcePath) {
        URL resource = ClasspathTrait.class.getClassLoader().getResource(resourcePath);

        if (resource == null) {
            throw new IllegalArgumentException("Could not find classpath resource at provided path: " + resourcePath);
        }

        String resourceFilePath = new File(resource.getFile()).getAbsolutePath();

        return ((SELF) this).withFileFromPath(path, Paths.get(resourceFilePath));
    }
}
