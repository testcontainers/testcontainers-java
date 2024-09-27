package org.testcontainers;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;

public abstract class AbstractJarFileTest {

    public static Path root;

    static {
        try {
            Path jarFilePath = Paths.get(System.getProperty("jarFile"));
            String decodedPath = URLDecoder.decode(jarFilePath.toUri().toString(), StandardCharsets.UTF_8.name());
            URI jarFileUri = new URI("jar", decodedPath, null);
            FileSystem fileSystem = FileSystems.newFileSystem(jarFileUri, Collections.emptyMap());
            root = fileSystem.getPath("/");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
