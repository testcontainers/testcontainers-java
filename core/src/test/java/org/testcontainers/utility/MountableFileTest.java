package org.testcontainers.utility;

import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.rnorth.visibleassertions.VisibleAssertions.assertFalse;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

public class MountableFileTest {

    @Test
    public void forClasspathResource() throws Exception {
        final MountableFile mountableFile = MountableFile.forClasspathResource("mappable-resource/test-resource.txt");

        performChecks(mountableFile);
    }

    @Test
    public void forClasspathResourceWithAbsolutePath() throws Exception {
        final MountableFile mountableFile = MountableFile.forClasspathResource("/mappable-resource/test-resource.txt");

        performChecks(mountableFile);
    }

    @Test
    public void forClasspathResourceFromJar() throws Exception {
        final MountableFile mountableFile = MountableFile.forClasspathResource("docker-java.properties");

        performChecks(mountableFile);
    }

    @Test
    public void forHostPath() throws Exception {
        final Path file = createTempFile("somepath");
        final MountableFile mountableFile = MountableFile.forHostPath(file.toString());

        performChecks(mountableFile);
    }

    @Test
    public void forHostPathWithSpaces() throws Exception {
        final Path file = createTempFile("some path");
        final MountableFile mountableFile = MountableFile.forHostPath(file.toString());

        performChecks(mountableFile);

        assertTrue("The resolved path contains the original space", mountableFile.getResolvedPath().contains(" "));assertFalse("The resolved path does not contain an escaped space", mountableFile.getResolvedPath().contains("\\ "));
    }

    /*
     *
     */

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

    private void performChecks(final MountableFile mountableFile) {
        final String mountablePath = mountableFile.getResolvedPath();
        assertTrue("The resolved path can be found", new File(mountablePath).exists());
        assertFalse("The resolved path does not contain any URL escaping", mountablePath.contains("%20"));
    }

}