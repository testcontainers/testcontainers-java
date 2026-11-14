package org.testcontainers;

import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class JarFileShadingTest extends AbstractJarFileTest {

    @Test
    void testPackages() throws Exception {
        assertThatFileList(root).containsOnly("org", "META-INF");

        // "publicsuffix" is the data-only resource directory of the shaded httpclient5
        // (effective_tld_names.dat), not relocated, just like in docker-java's zerodep jar
        assertThatFileList(root.resolve("org")).containsOnly("testcontainers", "publicsuffix");
    }

    @Test
    void testMetaInf() throws Exception {
        assertThatFileList(root.resolve("META-INF"))
            .containsOnly(
                "MANIFEST.MF",
                "services",
                "versions",
                "native-image",
                "Schubfach-LICENSE",
                "FastDoubleParser-ThirdParty-LICENSE",
                "FastDoubleParser-LICENSE"
            );
    }

    @Test
    void testMetaInfServices() throws Exception {
        assertThatFileList(root.resolve("META-INF").resolve("services"))
            .allMatch(it -> it.startsWith("org.testcontainers."));
    }

    @Test
    void testHttpClientWireLoggersAreNotRelocated() throws Exception {
        // The shaded Apache HttpClient must send wire/header output to the plain
        // "org.apache.hc.client5.http.wire" / "org.apache.hc.client5.http.headers" loggers.
        // The logger names are plain string constants, and if they were relocated along with
        // the code (to "org.testcontainers.shaded..."), the loggers would inherit the level of
        // the `org.testcontainers` logger: raising it to DEBUG (the documented troubleshooting
        // switch in docs/supported_docker_environment/logging_config.md) would then print the
        // full Docker API traffic, including registry credentials. Wire logging must stay
        // opt-in on the exact, unrelocated logger names.
        Path shadedHttpClientClasses = root
            .resolve("org/testcontainers/shaded/org/apache/hc")
            .toAbsolutePath()
            .normalize();

        try (Stream<Path> entries = Files.walk(shadedHttpClientClasses)) {
            List<Path> shadedClasses = entries
                .filter(path -> path.toString().endsWith(".class"))
                .collect(Collectors.toList());

            assertThat(shadedClasses).isNotEmpty();

            for (Path shadedClass : shadedClasses) {
                String constantPool = new String(Files.readAllBytes(shadedClass), StandardCharsets.ISO_8859_1);
                assertThat(constantPool)
                    .as("constant pool of %s", shadedClass)
                    .doesNotContain("org.testcontainers.shaded.org.apache.hc.client5.http.wire")
                    .doesNotContain("org.testcontainers.shaded.org.apache.hc.client5.http.headers");
            }
        }

        // the wire/header logger names must keep their original, opt-in values
        String connectionClass = new String(
            Files.readAllBytes(
                root.resolve(
                    "org/testcontainers/shaded/org/apache/hc/client5/http/impl/io/DefaultManagedHttpClientConnection.class"
                )
            ),
            StandardCharsets.ISO_8859_1
        );
        assertThat(connectionClass)
            .contains("org.apache.hc.client5.http.wire")
            .contains("org.apache.hc.client5.http.headers");
    }

    private ListAssert<String> assertThatFileList(Path path) throws IOException {
        return (ListAssert) assertThat(Files.list(path))
            .extracting(Path::getFileName)
            .extracting(Path::toString)
            .extracting(it -> it.endsWith("/") ? it.substring(0, it.length() - 1) : it);
    }
}
