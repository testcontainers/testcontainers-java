package org.testcontainers.images.builder;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BuildContextTest {

    @TempDir
    Path context;

    /**
     * A Gradle cache used as build context, with the Dockerfile in a subdirectory and the cache's lock files excluded.
     */
    @Test
    void shouldSendBuildContextDirectoryHonoringDockerignore() throws IOException {
        write("caches/modules-2/files-2.1/org.example/lib/1.0/lib-1.0.jar", "jar");
        write("caches/modules-2/modules-2.lock", "lock");
        write("caches/journal-1/journal-1.lock", "lock");
        write("caches/journal-1/file-access.bin", "bin");
        write(".dockerignore", "**/*.lock\n");
        write("docker/Dockerfile", "FROM alpine:3.16\nCOPY caches /gradle/caches\n");

        ImageFromDockerfile image = new ImageFromDockerfile()
            .withBuildContext(context)
            .withDockerfile(context.resolve("docker/Dockerfile"));

        assertThat(listFiles(image, "/gradle"))
            .contains("/gradle/caches/modules-2/files-2.1/org.example/lib/1.0/lib-1.0.jar")
            .contains("/gradle/caches/journal-1/file-access.bin")
            .doesNotContain(".lock");
    }

    @Test
    void shouldDefaultToDockerfileInBuildContextRoot() throws IOException {
        write("Dockerfile", "FROM alpine:3.16\nCOPY . /context\n");
        write("included.txt", "included");
        write("excluded.txt", "excluded");
        write(".dockerignore", "excluded.txt\n");

        ImageFromDockerfile image = new ImageFromDockerfile().withBuildContext(context);

        assertThat(listFiles(image, "/context")).contains("/context/included.txt").doesNotContain("excluded.txt");
    }

    @Test
    void shouldRejectDockerfileOutsideBuildContext(@TempDir Path other) throws IOException {
        Path dockerfile = Files.write(other.resolve("Dockerfile"), "FROM alpine:3.16\n".getBytes());

        ImageFromDockerfile image = new ImageFromDockerfile().withBuildContext(context).withDockerfile(dockerfile);

        assertThatThrownBy(image::get).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectBuildContextCombinedWithFiles() throws IOException {
        write("Dockerfile", "FROM alpine:3.16\n");

        ImageFromDockerfile image = new ImageFromDockerfile()
            .withBuildContext(context)
            .withFileFromString("extra.txt", "extra");

        assertThatThrownBy(image::get).isInstanceOf(IllegalStateException.class);
    }

    private void write(String relativePath, String content) throws IOException {
        Path file = context.resolve(relativePath);
        Files.createDirectories(file.getParent());
        Files.write(file, content.getBytes());
    }

    @SuppressWarnings("resource")
    private static String listFiles(ImageFromDockerfile image, String directory) {
        try (
            GenericContainer<?> container = new GenericContainer<>(DockerImageName.parse(image.get()))
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
                .withCommand("find", directory, "-type", "f")
        ) {
            container.start();
            return container.getLogs();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
