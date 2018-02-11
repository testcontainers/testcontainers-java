package org.testcontainers;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import static java.util.Collections.emptyMap;

public abstract class AbstractJarFileTest {

    public static Path root;

    static {
        try {
            FileSystem fileSystem = FileSystems.newFileSystem(URI.create("jar:file://" + System.getProperty("jarFile")), emptyMap());
            root = fileSystem.getPath("/");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
