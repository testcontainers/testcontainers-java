package org.testcontainers;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Collections.emptyMap;

public abstract class AbstractJarFileTest {

    public static Path root;

    static {
        Path path = Paths.get("..", "..", "core", "target", "testcontainers-0-SNAPSHOT.jar");

        try {
            FileSystem fileSystem = FileSystems.newFileSystem(URI.create("jar:" + path.toUri()), emptyMap());
            root = fileSystem.getPath("/");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
