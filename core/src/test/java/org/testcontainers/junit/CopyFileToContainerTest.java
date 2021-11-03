package org.testcontainers.junit;

import com.google.common.io.Files;
import com.google.common.io.Resources;
import org.junit.Before;
import org.junit.Test;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.SelinuxContext;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertArrayEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertFalse;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;
import static org.testcontainers.TestImages.TINY_IMAGE;

public class CopyFileToContainerTest {
    public static String destinationOnHost;
    private static String directoryInContainer = "/tmp/mappable-resource/";
    private static String fileName = "test-resource.txt";

    @Before
    public void setup() throws IOException {
        destinationOnHost = File.createTempFile("testcontainers", null).getAbsolutePath();
    }

    @Test
    public void checkFileCopied() throws IOException, InterruptedException {
        // copyToContainer {
        try (
            GenericContainer<?> container = new GenericContainer<>(TINY_IMAGE)
                .withCommand("sleep", "3000")
                // withCopyFileToContainer ensure that a file or directory will be copied to the container
                // before starting. In this case, we map a classpath directory to a directory inside the container
                .withCopyFileToContainer(MountableFile.forClasspathResource("/mappable-resource/"), directoryInContainer)
        ) {
            container.start();

            // at this point directoryInContainer should exist, and should contain copies of file(s)
            String filesList = container.execInContainer("ls", directoryInContainer).getStdout();
            assertTrue("file list contains the file", filesList.contains(fileName));

        // }

            // copyFileFromContainer {
            container.copyFileFromContainer(directoryInContainer + fileName, destinationOnHost);
            // }
        }

        assertArrayEquals(
            Files.toByteArray(new File(destinationOnHost)),
            Resources.toByteArray(CopyFileToContainerTest.class.getResource("/mappable-resource/" + fileName))
        );
    }

    @Test
    public void shouldUseCopyForReadOnlyClasspathResources() throws Exception {
        try (
            GenericContainer<?> container = new GenericContainer<>(TINY_IMAGE)
                .withCommand("sleep", "3000")
                .withClasspathResourceMapping("/mappable-resource/", directoryInContainer, BindMode.READ_ONLY)
        ) {
            container.start();
            String filesList = container.execInContainer("ls", "/tmp/mappable-resource").getStdout();
            assertTrue("file list contains the file", filesList.contains(fileName));
        }
    }

    @Test
    public void shouldUseCopyOnlyWithReadOnlyClasspathResources() {
        String resource = "/test_copy_to_container.txt";
        GenericContainer<?> container = new GenericContainer<>(TINY_IMAGE)
            .withClasspathResourceMapping(resource, "/readOnly", BindMode.READ_ONLY)
            .withClasspathResourceMapping(resource, "/readOnlyNoSelinux", BindMode.READ_ONLY)

            .withClasspathResourceMapping(resource, "/readOnlyShared", BindMode.READ_ONLY, SelinuxContext.SHARED)
            .withClasspathResourceMapping(resource, "/readWrite", BindMode.READ_WRITE);

        Map<MountableFile, String> copyMap = container.getCopyToFileContainerPathMap();
        assertTrue("uses copy for read-only", copyMap.containsValue("/readOnly"));
        assertTrue("uses copy for read-only and no Selinux", copyMap.containsValue("/readOnlyNoSelinux"));

        assertFalse("uses mount for read-only with Selinux", copyMap.containsValue("/readOnlyShared"));
        assertFalse("uses mount for read-write", copyMap.containsValue("/readWrite"));
    }

    @Test
    public void shouldCreateFoldersStructureWithCopy() throws Exception {
        String resource = "/test_copy_to_container.txt";
        try (
            GenericContainer<?> container = new GenericContainer<>(TINY_IMAGE)
                .withCommand("sleep", "3000")
                .withClasspathResourceMapping(resource, "/a/b/c/file", BindMode.READ_ONLY)
        ) {
            container.start();
            String filesList = container.execInContainer("ls", "/a/b/c/").getStdout();
            assertTrue("file list contains the file", filesList.contains("file"));
        }
    }
}
