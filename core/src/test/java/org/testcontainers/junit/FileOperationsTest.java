package org.testcontainers.junit;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.rnorth.visibleassertions.VisibleAssertions.assertFalse;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;
import static org.testcontainers.TestImages.ALPINE_IMAGE;

public class FileOperationsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void copyFileToContainerFileTest() throws Exception {
        try (
            GenericContainer alpineCopyToContainer = new GenericContainer(ALPINE_IMAGE)
                .withCommand("top")
        ) {
            alpineCopyToContainer.start();
            final MountableFile mountableFile = MountableFile.forClasspathResource("test_copy_to_container.txt");
            alpineCopyToContainer.copyFileToContainer(mountableFile, "/test.txt");

            File actualFile = new File(temporaryFolder.getRoot().getAbsolutePath() + "/test_copy_to_container.txt");
            alpineCopyToContainer.copyFileFromContainer("/test.txt", actualFile.getPath());

            File expectedFile = new File(mountableFile.getResolvedPath());
            assertTrue("Files aren't same ", FileUtils.contentEquals(expectedFile, actualFile));
        }
    }

    @Test
    public void copyFileToContainerFolderTest() throws Exception {
        try (
            GenericContainer alpineCopyToContainer = new GenericContainer(ALPINE_IMAGE)
                .withCommand("top")
        ) {
            alpineCopyToContainer.start();
            final MountableFile mountableFile = MountableFile.forClasspathResource("test_copy_to_container.txt");
            alpineCopyToContainer.copyFileToContainer(mountableFile, "/home/");

            File actualFile = new File(temporaryFolder.getRoot().getAbsolutePath() + "/test_copy_to_container.txt");
            alpineCopyToContainer.copyFileFromContainer("/home/test_copy_to_container.txt", actualFile.getPath());

            File expectedFile = new File(mountableFile.getResolvedPath());
            assertTrue("Files aren't same ", FileUtils.contentEquals(expectedFile, actualFile));
        }
    }

    @Test
    public void copyFolderToContainerFolderTest() throws Exception {
        try (
            GenericContainer alpineCopyToContainer = new GenericContainer(ALPINE_IMAGE)
                .withCommand("top")
        ) {

            alpineCopyToContainer.start();
            final MountableFile mountableFile = MountableFile.forClasspathResource("mappable-resource/");
            alpineCopyToContainer.copyFileToContainer(mountableFile, "/home/test/");

            File actualFile = new File(temporaryFolder.getRoot().getAbsolutePath() + "/test_copy_to_container.txt");
            alpineCopyToContainer.copyFileFromContainer("/home/test/test-resource.txt", actualFile.getPath());

            File expectedFile = new File(mountableFile.getResolvedPath() + "/test-resource.txt");
            assertTrue("Files aren't same ", FileUtils.contentEquals(expectedFile, actualFile));
        }
    }

    @Test(expected = NotFoundException.class)
    public void copyFromContainerShouldFailBecauseNoFileTest() throws NotFoundException {
        try (
            GenericContainer alpineCopyToContainer = new GenericContainer(ALPINE_IMAGE)
                .withCommand("top")
        ) {
            alpineCopyToContainer.start();
            alpineCopyToContainer.copyFileFromContainer("/home/test.txt", "src/test/resources/copy-from/test.txt");
        }
    }

    @Test
    public void shouldCopyFileFromContainerTest() throws IOException {
        try (
            GenericContainer alpineCopyToContainer = new GenericContainer(ALPINE_IMAGE)
                .withCommand("top")
        ) {

            alpineCopyToContainer.start();
            final MountableFile mountableFile = MountableFile.forClasspathResource("test_copy_to_container.txt");
            alpineCopyToContainer.copyFileToContainer(mountableFile, "/home/");

            File actualFile = new File(temporaryFolder.getRoot().getAbsolutePath() + "/test_copy_from_container.txt");
            alpineCopyToContainer.copyFileFromContainer("/home/test_copy_to_container.txt", actualFile.getPath());

            File expectedFile = new File(mountableFile.getResolvedPath());
            assertTrue("Files aren't same ", FileUtils.contentEquals(expectedFile, actualFile));
        }
    }

    @Test(expected = IllegalStateException.class)
    public void copyFileToNotStartedContainerShouldFailNicely() {
        try (
            GenericContainer alpineCopyToContainer = new GenericContainer(ALPINE_IMAGE)
                .withCommand("top")
        ) {
            final MountableFile mountableFile = MountableFile.forClasspathResource("test_copy_to_container.txt");
            alpineCopyToContainer.copyFileToContainer(mountableFile, "/home/");
        }
    }

    @Test(expected = IllegalStateException.class)
    public void copyFileFromNotStartedContainerShouldFailNicely() {
        try (
            GenericContainer alpineCopyToContainer = new GenericContainer(ALPINE_IMAGE)
                .withCommand("top")
        ) {
            File actualFile = new File(temporaryFolder.getRoot().getAbsolutePath() + "/test_copy_from_container.txt");
            alpineCopyToContainer.copyFileFromContainer("/home/test_copy_to_container.txt", actualFile.getPath());
        }
    }

    /**
     * This is expected to fail because {@link GenericContainer#stop()} stops _and removes_ the container
     */
    @Test(expected = IllegalStateException.class)
    public void copyFileFromKnownStoppedContainerShouldFailNicely() {
        try (
            GenericContainer alpineCopyToContainer = new GenericContainer(ALPINE_IMAGE)
                .withCommand("top")
        ) {
            alpineCopyToContainer.start();

            final MountableFile mountableFile = MountableFile.forClasspathResource("test_copy_to_container.txt");
            alpineCopyToContainer.copyFileToContainer(mountableFile, "/home/");
            assertTrue("The container is still running", alpineCopyToContainer.getCurrentContainerInfo().getState().getRunning());

            alpineCopyToContainer.stop();

            File actualFile = new File(temporaryFolder.getRoot().getAbsolutePath() + "/test_copy_from_container.txt");
            alpineCopyToContainer.copyFileFromContainer("/home/test_copy_to_container.txt", actualFile.getPath());
        }
    }

    @Test
    public void shouldCopyFileFromExitedContainerTest() throws IOException {
        String testFileContent = this.getClass().getCanonicalName();

        // There is a sleep in here because container.start() fails intermittently if the container exits too quickly
        String command = String.format("echo '%s' > /home/file_in_container.txt && sleep 3", testFileContent);

        try (
            GenericContainer alpineCopyToContainer = new GenericContainer(ALPINE_IMAGE)
                .withCommand("sh", "-c", command)
        ) {
            alpineCopyToContainer.start();

            while (alpineCopyToContainer.getCurrentContainerInfo().getState().getRunning()) {
                // container is still running, wait for it to exit
            }

            InspectContainerResponse currentContainerInfo = alpineCopyToContainer.getCurrentContainerInfo();
            assertFalse("The container is no longer running", currentContainerInfo.getState().getRunning());
            assertTrue("The container status is exited", "exited".equals(currentContainerInfo.getState().getStatus()));

            File actualFile = new File(temporaryFolder.getRoot().getAbsolutePath() + "/test_copy_from_container.txt");
            alpineCopyToContainer.copyFileFromContainer("/home/file_in_container.txt", actualFile.getPath());
            byte[] bytes = Files.readAllBytes(actualFile.toPath());
            assertTrue("Files aren't same ", (testFileContent + "\n").equals(new String(bytes)));
        }
    }

    @Test
    public void shouldCopyFileFromFailedContainer() throws IOException {
        String testFileContent = this.getClass().getCanonicalName();

        // There is a sleep in here because container.start() fails intermittently if the container exits too quickly
        String command = String.format("echo '%s' > /home/file_in_container.txt && sleep 3 && exit 1", testFileContent);
        try (
            GenericContainer alpineCopyToContainer = new GenericContainer(ALPINE_IMAGE)
                .withCommand("sh", "-c", command)
        ) {
            alpineCopyToContainer.start();

            while (alpineCopyToContainer.getCurrentContainerInfo().getState().getRunning()) {
                // container is still running, wait for it to exit
            }

            InspectContainerResponse currentContainerInfo = alpineCopyToContainer.getCurrentContainerInfo();
            assertFalse("The container is no longer running", currentContainerInfo.getState().getRunning());
            assertTrue("The container status is exited", "exited".equals(currentContainerInfo.getState().getStatus()));

            File actualFile = new File(temporaryFolder.getRoot().getAbsolutePath() + "/test_copy_from_container.txt");
            alpineCopyToContainer.copyFileFromContainer("/home/file_in_container.txt", actualFile.getPath());
            byte[] bytes = Files.readAllBytes(actualFile.toPath());
            assertTrue("Files aren't same ", (testFileContent + "\n").equals(new String(bytes)));
        }
    }

    @Test
    public void shouldCopyFileFromStoppedContainer() throws IOException {
        try (
            GenericContainer alpineCopyToContainer = new GenericContainer(ALPINE_IMAGE)
                .withCommand("top")
        ) {
            alpineCopyToContainer.start();

            final MountableFile mountableFile = MountableFile.forClasspathResource("test_copy_to_container.txt");
            alpineCopyToContainer.copyFileToContainer(mountableFile, "/home/");
            assertTrue("The container is still running", alpineCopyToContainer.getCurrentContainerInfo().getState().getRunning());

            // Stop the container but directly via the docker client, bypassing the Test Containers framework
            alpineCopyToContainer.getDockerClient().stopContainerCmd(alpineCopyToContainer.getContainerId()).exec();
            while (alpineCopyToContainer.getCurrentContainerInfo().getState().getRunning()) {
                // container is still running, wait for it to exit
            }

            InspectContainerResponse currentContainerInfo = alpineCopyToContainer.getCurrentContainerInfo();
            assertFalse("The container is no longer running", currentContainerInfo.getState().getRunning());
            assertTrue("The container status is exited", "exited".equals(currentContainerInfo.getState().getStatus()));

            File actualFile = new File(temporaryFolder.getRoot().getAbsolutePath() + "/test_copy_from_container.txt");
            alpineCopyToContainer.copyFileFromContainer("/home/test_copy_to_container.txt", actualFile.getPath());

            File expectedFile = new File(mountableFile.getResolvedPath());
            assertTrue("Files aren't same ", FileUtils.contentEquals(expectedFile, actualFile));
        }
    }

    @Test
    public void shouldCopyFileFromPausedContainer() throws IOException {
        try (
            GenericContainer alpineCopyToContainer = new GenericContainer(ALPINE_IMAGE)
                .withCommand("top")
        ) {
            alpineCopyToContainer.start();
            final MountableFile mountableFile = MountableFile.forClasspathResource("test_copy_to_container.txt");
            alpineCopyToContainer.copyFileToContainer(mountableFile, "/home/");
            assertTrue("The container is still running", alpineCopyToContainer.getCurrentContainerInfo().getState().getRunning());

            // Pause the container directly via the docker client - there is no way to do a pause directly on a GenericContainer instance
            alpineCopyToContainer.getDockerClient().pauseContainerCmd(alpineCopyToContainer.getContainerId()).exec();
            while (!alpineCopyToContainer.getCurrentContainerInfo().getState().getPaused()) {
                // container is not yet paused, wait for it to pause
            }

            InspectContainerResponse currentContainerInfo = alpineCopyToContainer.getCurrentContainerInfo();
            assertTrue("The container status is paused", "paused".equals(currentContainerInfo.getState().getStatus()));

            File actualFile = new File(temporaryFolder.getRoot().getAbsolutePath() + "/test_copy_from_container.txt");
            alpineCopyToContainer.copyFileFromContainer("/home/test_copy_to_container.txt", actualFile.getPath());

            File expectedFile = new File(mountableFile.getResolvedPath());
            assertTrue("Files aren't same ", FileUtils.contentEquals(expectedFile, actualFile));
        }
    }

    /**
     * This is an ugly failure - but if a container is stopped and removed while a test is running what else would we expect to happen?
     */
    @Test(expected = RuntimeException.class)
    public void copyFileFromRemovedContainerShouldFail() {
        try (
            GenericContainer alpineCopyToContainer = new GenericContainer(ALPINE_IMAGE)
                .withCommand("top")
        ) {
            alpineCopyToContainer.start();

            final MountableFile mountableFile = MountableFile.forClasspathResource("test_copy_to_container.txt");
            alpineCopyToContainer.copyFileToContainer(mountableFile, "/home/");
            assertTrue("The container is still running", alpineCopyToContainer.getCurrentContainerInfo().getState().getRunning());

            alpineCopyToContainer.getDockerClient().stopContainerCmd(alpineCopyToContainer.getContainerId()).exec();
            alpineCopyToContainer.getDockerClient().removeContainerCmd(alpineCopyToContainer.getContainerId()).exec();

            File actualFile = new File(temporaryFolder.getRoot().getAbsolutePath() + "/test_copy_from_container.txt");
            alpineCopyToContainer.copyFileFromContainer("/home/test_copy_to_container.txt", actualFile.getPath());
        }
    }

    @Test
    public void copyFileToStoppedContainerFileTest() throws Exception {
        try (
            GenericContainer alpineCopyToContainer = new GenericContainer(ALPINE_IMAGE)
                .withCommand("top")
        ) {
            alpineCopyToContainer.start();

            // Stop the container but directly via the docker client, bypassing the Test Containers framework
            alpineCopyToContainer.getDockerClient().stopContainerCmd(alpineCopyToContainer.getContainerId()).exec();
            while (alpineCopyToContainer.getCurrentContainerInfo().getState().getRunning()) {
                // container is still running, wait for it to exit
            }

            final MountableFile mountableFile = MountableFile.forClasspathResource("test_copy_to_container.txt");
            alpineCopyToContainer.copyFileToContainer(mountableFile, "/test.txt");

            File actualFile = new File(temporaryFolder.getRoot().getAbsolutePath() + "/test_copy_to_container.txt");
            alpineCopyToContainer.copyFileFromContainer("/test.txt", actualFile.getPath());

            File expectedFile = new File(mountableFile.getResolvedPath());
            assertTrue("Files aren't same ", FileUtils.contentEquals(expectedFile, actualFile));
        }
    }

    @Test
    public void copyFileToStoppedContainerFolderTest() throws Exception {
        try (
            GenericContainer alpineCopyToContainer = new GenericContainer(ALPINE_IMAGE)
                .withCommand("top")
        ) {
            alpineCopyToContainer.start();

            // Stop the container but directly via the docker client, bypassing the Test Containers framework
            alpineCopyToContainer.getDockerClient().stopContainerCmd(alpineCopyToContainer.getContainerId()).exec();
            while (alpineCopyToContainer.getCurrentContainerInfo().getState().getRunning()) {
                // container is still running, wait for it to exit
            }

            final MountableFile mountableFile = MountableFile.forClasspathResource("test_copy_to_container.txt");
            alpineCopyToContainer.copyFileToContainer(mountableFile, "/home/");

            File actualFile = new File(temporaryFolder.getRoot().getAbsolutePath() + "/test_copy_to_container.txt");
            alpineCopyToContainer.copyFileFromContainer("/home/test_copy_to_container.txt", actualFile.getPath());

            File expectedFile = new File(mountableFile.getResolvedPath());
            assertTrue("Files aren't same ", FileUtils.contentEquals(expectedFile, actualFile));
        }
    }

    @Test
    public void copyFolderToStoppedContainerFolderTest() throws Exception {
        try (
            GenericContainer alpineCopyToContainer = new GenericContainer(ALPINE_IMAGE)
                .withCommand("top")
        ) {

            alpineCopyToContainer.start();

            // Stop the container but directly via the docker client, bypassing the Test Containers framework
            alpineCopyToContainer.getDockerClient().stopContainerCmd(alpineCopyToContainer.getContainerId()).exec();
            while (alpineCopyToContainer.getCurrentContainerInfo().getState().getRunning()) {
                // container is still running, wait for it to exit
            }

            final MountableFile mountableFile = MountableFile.forClasspathResource("mappable-resource/");
            alpineCopyToContainer.copyFileToContainer(mountableFile, "/home/test/");

            File actualFile = new File(temporaryFolder.getRoot().getAbsolutePath() + "/test_copy_to_container.txt");
            alpineCopyToContainer.copyFileFromContainer("/home/test/test-resource.txt", actualFile.getPath());

            File expectedFile = new File(mountableFile.getResolvedPath() + "/test-resource.txt");
            assertTrue("Files aren't same ", FileUtils.contentEquals(expectedFile, actualFile));
        }
    }

    @Test
    public void copyFileToExitedContainerFileTest() throws Exception {
        try (
            GenericContainer alpineCopyToContainer = new GenericContainer(ALPINE_IMAGE)
                .withCommand("sleep", "3")
        ) {
            alpineCopyToContainer.start();

            while (alpineCopyToContainer.getCurrentContainerInfo().getState().getRunning()) {
                // container is still running, wait for it to exit
            }

            final MountableFile mountableFile = MountableFile.forClasspathResource("test_copy_to_container.txt");
            alpineCopyToContainer.copyFileToContainer(mountableFile, "/test.txt");

            File actualFile = new File(temporaryFolder.getRoot().getAbsolutePath() + "/test_copy_to_container.txt");
            alpineCopyToContainer.copyFileFromContainer("/test.txt", actualFile.getPath());

            File expectedFile = new File(mountableFile.getResolvedPath());
            assertTrue("Files aren't same ", FileUtils.contentEquals(expectedFile, actualFile));
        }
    }

    @Test
    public void copyFileToFailedContainerFileTest() throws Exception {
        try (
            GenericContainer alpineCopyToContainer = new GenericContainer(ALPINE_IMAGE)
                .withCommand("sh", "-c", "sleep 3 && exit 1")
        ) {
            alpineCopyToContainer.start();

            while (alpineCopyToContainer.getCurrentContainerInfo().getState().getRunning()) {
                // container is still running, wait for it to exit
            }

            final MountableFile mountableFile = MountableFile.forClasspathResource("test_copy_to_container.txt");
            alpineCopyToContainer.copyFileToContainer(mountableFile, "/test.txt");

            File actualFile = new File(temporaryFolder.getRoot().getAbsolutePath() + "/test_copy_to_container.txt");
            alpineCopyToContainer.copyFileFromContainer("/test.txt", actualFile.getPath());

            File expectedFile = new File(mountableFile.getResolvedPath());
            assertTrue("Files aren't same ", FileUtils.contentEquals(expectedFile, actualFile));
        }
    }

    @Test
    public void copyFileToPausedContainerFileTest() throws Exception {
        try (
            GenericContainer alpineCopyToContainer = new GenericContainer(ALPINE_IMAGE)
                .withCommand("top")
        ) {
            alpineCopyToContainer.start();

            // Stop the container but directly via the docker client, bypassing the Test Containers framework
            alpineCopyToContainer.getDockerClient().pauseContainerCmd(alpineCopyToContainer.getContainerId()).exec();
            while (!alpineCopyToContainer.getCurrentContainerInfo().getState().getPaused()) {
                // container is not yet paused, wait for it to pause
            }

            final MountableFile mountableFile = MountableFile.forClasspathResource("test_copy_to_container.txt");
            alpineCopyToContainer.copyFileToContainer(mountableFile, "/test.txt");

            File actualFile = new File(temporaryFolder.getRoot().getAbsolutePath() + "/test_copy_to_container.txt");
            alpineCopyToContainer.copyFileFromContainer("/test.txt", actualFile.getPath());

            File expectedFile = new File(mountableFile.getResolvedPath());
            assertTrue("Files aren't same ", FileUtils.contentEquals(expectedFile, actualFile));
        }
    }
}
