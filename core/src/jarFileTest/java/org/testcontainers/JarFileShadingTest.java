package org.testcontainers;

import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class JarFileShadingTest extends AbstractJarFileTest {

    @Test
    void testPackages() throws Exception {
        assertThatFileList(root).containsOnly("org", "META-INF");

        assertThatFileList(root.resolve("org")).containsOnly("testcontainers");
    }

    @Test
    void testMetaInf() throws Exception {
        assertThatFileList(root.resolve("META-INF"))
            .containsOnly(
                "MANIFEST.MF",
                "services",
                "versions",
                "native-image",
                "thirdparty-LICENSE",
                "FastDoubleParser-NOTICE",
                "FastDoubleParser-LICENSE"
            );
    }

    @Test
    void testMetaInfServices() throws Exception {
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
