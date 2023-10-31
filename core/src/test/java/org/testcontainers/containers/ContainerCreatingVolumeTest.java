package org.testcontainers.containers;

import junit.framework.TestCase;
import org.junit.Test;
import org.testcontainers.TestImages;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class ContainerCreatingVolumeTest {

    @Test
    public void shouldCreateANewVolumeAndShareItWithAnotherContainer() {
        GenericContainer<?> container = new GenericContainer<>(TestImages.TINY_IMAGE)
            .withCommand("sh", "-c", "echo \"test\">/workdir/test.txt;")
            .withVolume("/workdir");

        container.start();
        GenericContainer<?> container2 = new GenericContainer<>(TestImages.TINY_IMAGE)
            .withCommand("sh", "-c", "cat /workdir/test.txt")
            .withVolumesFrom(container, BindMode.READ_ONLY);
        container2.start();
        TestCase.assertEquals("test", container2.getLogs().replaceAll("\n", ""));
        container2.stop();
        container.stop();
    }

    @Test
    public void shouldCreateANewVolumeStopTheContainerAndStartAnotherContainer() throws IOException {
        File tempDirectory = Files.createTempDirectory("test").toFile();
        GenericContainer<?> container = new GenericContainer<>(TestImages.TINY_IMAGE)
            .withCommand("sh", "-c", "echo \"test\">/workdir/test.txt;");

        container.addFileSystemBind(tempDirectory.getAbsolutePath(), "/workdir", BindMode.READ_WRITE);

        container.start();
        container.stop();

        TestCase.assertTrue(tempDirectory.exists() && tempDirectory.isDirectory());
        TestCase.assertEquals(1, tempDirectory.listFiles().length);
        File tempFile = tempDirectory.listFiles()[0];
        TestCase.assertTrue(new File(tempDirectory, "test.txt").exists());
        TestCase.assertEquals(
            "test",
            Files
                .readAllLines(tempFile.toPath(), java.nio.charset.StandardCharsets.UTF_8)
                .stream()
                .filter(s -> !s.isEmpty())
                .findFirst()
                .orElse("")
        );

        GenericContainer<?> container2 = new GenericContainer<>(TestImages.TINY_IMAGE)
            .withCommand("sh", "-c", "cat /workdir/test.txt");

        container2.addFileSystemBind(tempDirectory.getAbsolutePath(), "/workdir", BindMode.READ_ONLY);
        container2.start();

        TestCase.assertEquals("test", container2.getLogs().replaceAll("\n", ""));
        container2.stop();
    }
}
