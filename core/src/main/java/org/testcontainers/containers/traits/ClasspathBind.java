package org.testcontainers.containers.traits;

import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.TestContainer;
import org.testcontainers.utility.SelfReference;

import java.net.URL;
import java.util.Optional;

public class ClasspathBind<SELF extends TestContainer<SELF>> extends FileSystemBind<SELF> {

    public interface Support<SELF extends TestContainer<SELF>> extends SelfReference<SELF> {

        /**
         * Map a resource (file or directory) on the classpath to a path inside the container.
         * This will only work if you are running your tests outside a Docker container.
         *
         * @param resourcePath  path to the resource on the classpath (relative to the classpath root; should not start with a leading slash)
         * @param containerPath path this should be mapped to inside the container
         * @param mode          access mode for the file
         * @return this
         */
        default SELF withClasspathResourceMapping(String resourcePath, String containerPath, BindMode mode) {
            return self().with(new ClasspathBind<>(resourcePath, containerPath, mode));
        }
    }

    public ClasspathBind(String resourcePath, String containerPath, BindMode mode) {
        // We have to use Optional.ofNullable here because you can't do anything before super() statement :(
        super(
                Optional.ofNullable(GenericContainer.class.getClassLoader().getResource(resourcePath))
                        .map(URL::getFile)
                        .orElseThrow(() -> new IllegalArgumentException("Could not find classpath resource at provided path: " + resourcePath)),
                containerPath,
                mode
        );
    }
}
