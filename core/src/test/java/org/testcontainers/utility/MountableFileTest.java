package org.testcontainers.utility;

import lombok.Cleanup;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class MountableFileTest {

    private static final int TEST_FILE_MODE = 0532;

    private static final int BASE_FILE_MODE = 0100000;

    private static final int BASE_DIR_MODE = 0040000;

    @Test
    void forClasspathResource() throws Exception {
        final MountableFile mountableFile = MountableFile.forClasspathResource("mappable-resource/test-resource.txt");

        performChecks(mountableFile);
    }

    @Test
    void forClasspathResourceWithAbsolutePath() throws Exception {
        final MountableFile mountableFile = MountableFile.forClasspathResource("/mappable-resource/test-resource.txt");

        performChecks(mountableFile);
    }

    @Test
    void forClasspathResourceFromJar() throws Exception {
        final MountableFile mountableFile = MountableFile.forClasspathResource("META-INF/dummy_unique_name.txt");

        performChecks(mountableFile);
    }

    @Test
    void forClasspathResourceFromJarWithAbsolutePath() throws Exception {
        final MountableFile mountableFile = MountableFile.forClasspathResource("/META-INF/dummy_unique_name.txt");

        performChecks(mountableFile);
    }

    @Test
    void forClasspathResourceFileInJarIsExtractedIntoItsOwnDirectory() throws Exception {
        // see #9423: a single-file classpath resource extracted from a JAR must end up inside a
        // directory created specifically for this extraction (preserving the resource's own path
        // within the JAR), rather than being written directly onto the temp *directory*'s own path.
        // Otherwise the extracted file's parent is the shared system temp directory, which breaks
        // any caller that treats the resolved path's parent as a self-contained context (e.g.
        // building an image from a Dockerfile loaded via MountableFile.forClasspathResource(...)).
        final Map<String, String> entries = new LinkedHashMap<>();
        entries.put("nested/inside/jar/Dockerfile", "FROM postgres\n");
        final Path jarFile = createJarWithEntries(entries);

        withJarOnClasspath(
            jarFile,
            () -> {
                final MountableFile mountableFile = MountableFile.forClasspathResource("nested/inside/jar/Dockerfile");
                final File extractedFile = new File(mountableFile.getFilesystemPath());

                assertThat(extractedFile).as("the resource was extracted to a real file").isFile();
                assertThat(Files.readString(extractedFile.toPath())).isEqualTo("FROM postgres\n");

                final File parentDir = extractedFile.getParentFile();
                assertThat(parentDir)
                    .as("the extracted file's parent is not the shared system temp directory")
                    .isNotEqualTo(new File(System.getProperty("java.io.tmpdir")));
                assertThat(parentDir.list())
                    .as("only the extracted resource lives in its own extraction directory")
                    .containsExactly("Dockerfile");
            }
        );
    }

    @Test
    void forClasspathResourceDirectoryInJarPreservesRelativeStructure() throws Exception {
        // Regression guard: extracting a directory resource from a JAR must still lay out its
        // files relative to the resolved path exactly as before this fix.
        final Map<String, String> entries = new LinkedHashMap<>();
        entries.put("assets/", "");
        entries.put("assets/dir/", "");
        entries.put("assets/dir/sub/", "");
        entries.put("assets/dir/a.txt", "a-content");
        entries.put("assets/dir/sub/b.txt", "b-content");
        final Path jarFile = createJarWithEntries(entries);

        withJarOnClasspath(
            jarFile,
            () -> {
                final MountableFile mountableFile = MountableFile.forClasspathResource("assets/dir");
                final String resolvedPath = mountableFile.getResolvedPath();

                assertThat(Files.readString(new File(resolvedPath, "a.txt").toPath())).isEqualTo("a-content");
                assertThat(Files.readString(new File(resolvedPath, "sub/b.txt").toPath())).isEqualTo("b-content");
            }
        );
    }

    @Test
    void forHostPath() throws Exception {
        final Path file = createTempFile("somepath");
        final MountableFile mountableFile = MountableFile.forHostPath(file.toString());

        performChecks(mountableFile);
    }

    @Test
    void forHostPathWithSpaces() throws Exception {
        final Path file = createTempFile("some path");
        final MountableFile mountableFile = MountableFile.forHostPath(file.toString());

        performChecks(mountableFile);

        assertThat(mountableFile.getResolvedPath()).as("The resolved path contains the original space").contains(" ");
        assertThat(mountableFile.getResolvedPath())
            .as("The resolved path does not contain an escaped space")
            .doesNotContain("\\ ");
    }

    @Test
    void forHostPathWithPlus() throws Exception {
        final Path file = createTempFile("some+path");
        final MountableFile mountableFile = MountableFile.forHostPath(file.toString());

        performChecks(mountableFile);

        assertThat(mountableFile.getResolvedPath()).as("The resolved path contains the original space").contains("+");
        assertThat(mountableFile.getResolvedPath())
            .as("The resolved path does not contain an escaped space")
            .doesNotContain(" ");
    }

    @Test
    void forClasspathResourceWithPermission() throws Exception {
        final MountableFile mountableFile = MountableFile.forClasspathResource(
            "mappable-resource/test-resource.txt",
            TEST_FILE_MODE
        );

        performChecks(mountableFile);
        assertThat(mountableFile.getFileMode()).as("Valid file mode.").isEqualTo(BASE_FILE_MODE | TEST_FILE_MODE);
    }

    @Test
    void forHostFilePathWithPermission() throws Exception {
        final Path file = createTempFile("somepath");
        final MountableFile mountableFile = MountableFile.forHostPath(file.toString(), TEST_FILE_MODE);
        performChecks(mountableFile);
        assertThat(mountableFile.getFileMode()).as("Valid file mode.").isEqualTo(BASE_FILE_MODE | TEST_FILE_MODE);
    }

    @Test
    void forHostDirPathWithPermission() throws Exception {
        final Path dir = createTempDir();
        final MountableFile mountableFile = MountableFile.forHostPath(dir.toString(), TEST_FILE_MODE);
        performChecks(mountableFile);
        assertThat(mountableFile.getFileMode()).as("Valid dir mode.").isEqualTo(BASE_DIR_MODE | TEST_FILE_MODE);
    }

    @Test
    void noTrailingSlashesInTarEntryNames() throws Exception {
        final MountableFile mountableFile = MountableFile.forClasspathResource("mappable-resource/test-resource.txt");

        @Cleanup
        final TarArchiveInputStream tais = intoTarArchive(taos -> {
            mountableFile.transferTo(taos, "/some/path.txt");
            mountableFile.transferTo(taos, "/path.txt");
            mountableFile.transferTo(taos, "path.txt");
        });

        ArchiveEntry entry;
        while ((entry = tais.getNextEntry()) != null) {
            assertThat(entry.getName()).as("no entries should have a trailing slash").doesNotEndWith("/");
        }
    }

    @NotNull
    private Path createJarWithEntries(final Map<String, String> entries) throws IOException {
        final Path jarFile = Files.createTempFile("mountable-file-test", ".jar");
        jarFile.toFile().deleteOnExit();

        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(jarFile))) {
            for (final Map.Entry<String, String> entry : entries.entrySet()) {
                jos.putNextEntry(new JarEntry(entry.getKey()));
                jos.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                jos.closeEntry();
            }
        }

        return jarFile;
    }

    private void withJarOnClasspath(final Path jarFile, final ThrowingRunnable runnable) throws Exception {
        final ClassLoader previousContextClassLoader = Thread.currentThread().getContextClassLoader();
        try (URLClassLoader jarClassLoader = new URLClassLoader(new URL[] { jarFile.toUri().toURL() }, previousContextClassLoader)) {
            Thread.currentThread().setContextClassLoader(jarClassLoader);
            try {
                runnable.run();
            } finally {
                Thread.currentThread().setContextClassLoader(previousContextClassLoader);
            }
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private TarArchiveInputStream intoTarArchive(Consumer<TarArchiveOutputStream> consumer) throws IOException {
        @Cleanup
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        @Cleanup
        final TarArchiveOutputStream taos = new TarArchiveOutputStream(baos);
        consumer.accept(taos);
        taos.close();

        return new TarArchiveInputStream(new ByteArrayInputStream(baos.toByteArray()));
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @NotNull
    private Path createTempFile(final String name) throws IOException {
        final File tempParentDir = File.createTempFile("testcontainers", "");
        tempParentDir.delete();
        tempParentDir.mkdirs();
        final Path file = new File(tempParentDir, name).toPath();

        Files.copy(MountableFileTest.class.getResourceAsStream("/mappable-resource/test-resource.txt"), file);
        return file;
    }

    @NotNull
    private Path createTempDir() throws IOException {
        return Files.createTempDirectory("testcontainers");
    }

    private void performChecks(final MountableFile mountableFile) {
        final String mountablePath = mountableFile.getResolvedPath();
        assertThat(new File(mountablePath)).as("The filesystem path '" + mountablePath + "' can be found").exists();
        assertThat(mountablePath)
            .as("The filesystem path '" + mountablePath + "' does not contain any URL escaping")
            .doesNotContain("%20");
    }
}
