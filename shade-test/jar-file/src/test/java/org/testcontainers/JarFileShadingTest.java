package org.testcontainers;

import org.assertj.core.api.ListAssert;
import org.junit.*;

import java.io.IOException;
import java.net.URI;
import java.nio.file.*;

import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;

public class JarFileShadingTest {

    private static FileSystem fileSystem;

    private static Path root;

    @BeforeClass
    public static void setUp() throws Exception {
        Path path = Paths.get("..", "..", "core", "target", "testcontainers-0-SNAPSHOT.jar");

        fileSystem = FileSystems.newFileSystem(URI.create("jar:" + path.toUri()), emptyMap());

        root = fileSystem.getRootDirectories().iterator().next();
    }

    @AfterClass
    public static void tearDown() throws Exception {
        fileSystem.close();
    }

    @Test
    public void testPackages() throws Exception {
        assertThatFileList(root).containsOnly(
                "docker-java.properties",
                "org/",
                "META-INF/",
                "com/"
        );

        assertThatFileList(root.resolve("org")).containsOnly(
                "testcontainers/"
        );

        assertThatFileList(root.resolve("com")).containsOnly(
                "github/"
        );

        assertThatFileList(root.resolve("com").resolve("github")).containsOnly(
                "dockerjava/"
        );
    }

    @Test
    public void testMetaInf() throws Exception {
        assertThatFileList(root.resolve("META-INF")).containsOnly(
                "MANIFEST.MF",
                "services/",
                "native/"
        );

        assertThatFileList(root.resolve("META-INF").resolve("native")).containsOnly(
                "liborg-testcontainers-shaded-netty-transport-native-epoll.so"
        );
    }

    @Test
    public void testMetaInfServices() throws Exception {
        assertThatFileList(root.resolve("META-INF").resolve("services"))
                .allMatch(it -> it.startsWith("org.testcontainers."));
    }

    private ListAssert<String> assertThatFileList(Path path) throws IOException {
        return (ListAssert) assertThat(Files.list(path))
                .extracting(Path::getFileName)
                .extracting(Object::toString);
    }
}
