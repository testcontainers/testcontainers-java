package org.testcontainers;

import org.assertj.core.api.ListAssert;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

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

    @Test
    void testShadedMapperReadsPublicDockerModelAnnotations() throws Exception {
        URL jar = Paths.get(System.getProperty("jarFile")).toUri().toURL();
        try (URLClassLoader loader = new URLClassLoader(new URL[] { jar }, getClass().getClassLoader())) {
            Class<?> config = loader.loadClass(
                "org.testcontainers.shaded.com.github.dockerjava.core.DockerClientConfig"
            );
            Object mapper = config.getMethod("getDefaultObjectMapper").invoke(null);
            Class<?> infoType = loader.loadClass("com.github.dockerjava.api.model.Info");
            Object info = mapper
                .getClass()
                .getMethod("readValue", String.class, Class.class)
                .invoke(
                    mapper,
                    "{\"ID\":\"docker-id\",\"Containers\":3,\"DockerRootDir\":\"/var/lib/docker\"}",
                    infoType
                );

            assertThat(infoType.getMethod("getId").invoke(info)).isEqualTo("docker-id");
            assertThat(infoType.getMethod("getContainers").invoke(info)).isEqualTo(3);
            assertThat(infoType.getMethod("getDockerRootDir").invoke(info)).isEqualTo("/var/lib/docker");
        }
    }

    private ListAssert<String> assertThatFileList(Path path) throws IOException {
        return (ListAssert) assertThat(Files.list(path))
            .extracting(Path::getFileName)
            .extracting(Path::toString)
            .extracting(it -> it.endsWith("/") ? it.substring(0, it.length() - 1) : it);
    }
}
