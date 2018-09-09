package org.testcontainers;

import org.assertj.core.api.ListAssert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

public class JarFileShadingTest extends AbstractJarFileTest {

    @Test
    public void testPackages() throws Exception {
        assertThatFileList(root).containsOnly(
                "docker-java.properties",
                "org",
                "META-INF",
                "com"
        );

        assertThatFileList(root.resolve("org")).containsOnly(
                "testcontainers"
        );

        assertThatFileList(root.resolve("com")).containsOnly(
                "github"
        );

        assertThatFileList(root.resolve("com").resolve("github")).containsOnly(
                "dockerjava"
        );
    }

    @Test
    public void testMetaInf() throws Exception {
        assertThatFileList(root.resolve("META-INF")).containsOnly(
                "MANIFEST.MF",
                "services",
                "native"
        );

        assertThatFileList(root.resolve("META-INF").resolve("native")).containsOnly(
                "liborg-testcontainers-shaded-netty_transport_native_epoll_x86_64.so",
                "liborg-testcontainers-shaded-netty_transport_native_kqueue_x86_64.jnilib"
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
                .extracting(Path::toString)
                .extracting(it -> it.endsWith("/") ? it.substring(0, it.length() - 1) : it);
    }
}
