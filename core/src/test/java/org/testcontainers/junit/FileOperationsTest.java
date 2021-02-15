package org.testcontainers.junit;

import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.exception.NotFoundException;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
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
    public void copyFileToContainerShouldFailWhenNotStartedTest() {

        try (GenericContainer container = new GenericContainer(ALPINE_IMAGE).withCommand("top")) {
            final MountableFile mountableFile = MountableFile.forClasspathResource("test_copy_to_container.txt");
            container.copyFileToContainer(mountableFile, "/home/");
        }
    }

    @Test(expected = IllegalStateException.class)
    public void copyFileFromContainerShouldFailWhenNotStartedTest() {

        try (GenericContainer container = new GenericContainer(ALPINE_IMAGE).withCommand("top")) {
            File actualFile = new File(temporaryFolder.getRoot().getAbsolutePath() + "/test_copy_from_container.txt");
            container.copyFileFromContainer("/home/test_copy_to_container.txt", actualFile.getPath());
        }
    }

    /**
     * This is expected to fail because {@link GenericContainer#stop()} stops _and removes_ the container
     */
    @Test(expected = IllegalStateException.class)
    public void copyFileFromContainerShouldFailWhenStoppedAndRemovedTest() {

        try (GenericContainer container = new GenericContainer(ALPINE_IMAGE).withCommand("top")) {
            container.start();

            final MountableFile mountableFile = MountableFile.forClasspathResource("test_copy_to_container.txt");
            container.copyFileToContainer(mountableFile, "/home/");
            assertTrue("The container is still running", container.getCurrentContainerInfo().getState().getRunning());

            container.stop();

            File actualFile = new File(temporaryFolder.getRoot().getAbsolutePath() + "/test_copy_from_container.txt");
            container.copyFileFromContainer("/home/test_copy_to_container.txt", actualFile.getPath());
        }
    }

    @Test
    public void shouldCopyFileFromExitedContainerTest() throws IOException {

        String testFileContent = getTestFileContent();
        // There is a sleep in here because container.start() fails intermittently if the container exits too quickly
        String command = String.format("echo -n '%s' > /home/file_in_container.txt && sleep 3", testFileContent);

        try (GenericContainer container = new GenericContainer(ALPINE_IMAGE).withCommand("sh", "-c", command)) {
            container.start();
            assertCopyFromStoppedContainerWorks(container, "/home/file_in_container.txt", 0);
        }
    }

    @Test
    public void shouldCopyFileToExitedContainerTest() throws Exception {

        try (GenericContainer container = new GenericContainer(ALPINE_IMAGE).withCommand("sleep", "3")) {
            container.start();

            assertCopyToStoppedContainerWorks(container, "/home/test_copy_to_container.txt", 0);
        }
    }

    @Test
    public void shouldCopyFileFromFailedContainerTest() throws IOException {

        String testFileContent = getTestFileContent();
        // There is a sleep in here because container.start() fails intermittently if the container exits too quickly
        String command = String.format("echo -n '%s' > /home/file_in_container.txt && sleep 3 && exit 1", testFileContent);

        try (GenericContainer container = new GenericContainer(ALPINE_IMAGE).withCommand("sh", "-c", command)) {
            container.start();
            assertCopyFromStoppedContainerWorks(container, "/home/file_in_container.txt", 1);
        }
    }

    @Test
    public void shouldCopyFileToFailedContainerTest() throws Exception {

        try (
            GenericContainer container = new GenericContainer(ALPINE_IMAGE).withCommand("sh", "-c", "sleep 3 && exit 1")
        ) {
            container.start();

            assertCopyToStoppedContainerWorks(container, "/home/test_copy_to_container.txt", 1);
        }
    }

    @Test
    public void shouldCopyFileFromStoppedContainerTest() throws IOException {

        try (GenericContainer container = new GenericContainer(ALPINE_IMAGE).withCommand("top")) {
            container.start();

            final MountableFile mountableFile = MountableFile.forClasspathResource("test_copy_to_container.txt");
            container.copyFileToContainer(mountableFile, "/home/");
            assertTrue("The container is still running", container.getCurrentContainerInfo().getState().getRunning());

            stopViaDockerClient(container);

            // 143 is Sigterm
            assertCopyFromStoppedContainerWorks(container, "/home/test_copy_to_container.txt", 143);
        }
    }

    @Test
    public void shouldCopyFileToStoppedContainerTest() throws Exception {

        try (GenericContainer container = new GenericContainer(ALPINE_IMAGE).withCommand("top")) {
            container.start();

            stopViaDockerClient(container);

            assertCopyToStoppedContainerWorks(container, "/home/test_copy_to_container.txt", 143);
        }
    }


    @Test
    public void shouldCopyFileFromPausedContainerTest() throws IOException {

        try (GenericContainer container = new GenericContainer(ALPINE_IMAGE).withCommand("top")) {
            container.start();
            final MountableFile mountableFile = MountableFile.forClasspathResource("test_copy_to_container.txt");
            container.copyFileToContainer(mountableFile, "/home/");
            assertTrue("The container is still running", container.getCurrentContainerInfo().getState().getRunning());

            // Pause the container directly via the docker client - there is no way to do a pause directly on a GenericContainer instance
            pauseViaDockerClient(container);

            InspectContainerResponse currentContainerInfo = container.getCurrentContainerInfo();
            assertTrue("The container status is paused", "paused".equals(currentContainerInfo.getState().getStatus()));

            File actualFile = new File(temporaryFolder.getRoot().getAbsolutePath() + "/test_copy_from_container.txt");
            container.copyFileFromContainer("/home/test_copy_to_container.txt", actualFile.getPath());

            File expectedFile = new File(mountableFile.getResolvedPath());
            assertTrue("Files aren't same ", FileUtils.contentEquals(expectedFile, actualFile));
        }
    }

    @Test
    public void shouldCopyFileToPausedContainerTest() throws Exception {

        try (GenericContainer container = new GenericContainer(ALPINE_IMAGE).withCommand("top")) {
            container.start();

            pauseViaDockerClient(container);

            final MountableFile mountableFile = MountableFile.forClasspathResource("test_copy_to_container.txt");
            container.copyFileToContainer(mountableFile, "/test.txt");

            File actualFile = new File(temporaryFolder.getRoot().getAbsolutePath() + "/test_copy_to_container.txt");
            container.copyFileFromContainer("/test.txt", actualFile.getPath());

            File expectedFile = new File(mountableFile.getResolvedPath());
            assertTrue("Files aren't same ", FileUtils.contentEquals(expectedFile, actualFile));
        }
    }

    /**
     * Utility method to retrieve the content of the file to copy to avoid duplication of content in tests
     *
     * @return String content of a file system resource
     */
    private static String getTestFileContent() throws IOException {

        try (InputStream in = FileOperationsTest.class.getResourceAsStream("/test_copy_to_container.txt")) {
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString(StandardCharsets.UTF_8.name());
        }
    }


    /**
     * Stop the container directly via the docker client, bypassing the Test Containers framework
     * @param container Container to stop
     */
    private static void stopViaDockerClient(GenericContainer container) {
        container.getDockerClient().stopContainerCmd(container.getContainerId()).exec();
    }

    /**
     * There is no public API to pause a container via test container, but some libraries might want to
     * manipulate a paused container as well. So we want to test for copying things to and from those as well.
     * We have to loop here as there is no status
     * @param container
     */
    private void pauseViaDockerClient(GenericContainer container) {

        container.getDockerClient().pauseContainerCmd(container.getContainerId()).exec();
        Instant waitingStarted = Instant.now();
        while (Duration.between(waitingStarted, Instant.now()).getSeconds() < 30) {
            if(container.getCurrentContainerInfo().getState().getPaused()) {
                return;
            }
        }
        throw new IllegalStateException("Could not pause container.");
    }

    private void assertCopyFromStoppedContainerWorks(GenericContainer container, String fileNameInContainer, int expectedExitCode) throws IOException {

        int exitCode = container.getDockerClient().waitContainerCmd(container.getContainerId()).start().awaitStatusCode();

        assertEquals("Status code doesn't equals expected code", expectedExitCode, exitCode);

        InspectContainerResponse currentContainerInfo = container.getCurrentContainerInfo();
        assertFalse("The container is no longer running", currentContainerInfo.getState().getRunning());
        assertTrue("The container status is exited", "exited".equals(currentContainerInfo.getState().getStatus()));

        File actualFile = new File(temporaryFolder.getRoot().getAbsolutePath() + "/test_copy_from_container.txt");
        container.copyFileFromContainer(fileNameInContainer, actualFile.getPath());

        final MountableFile mountableFile = MountableFile.forClasspathResource("test_copy_to_container.txt");
        File expectedFile = new File(mountableFile.getResolvedPath());
        assertTrue("Files aren't same ", FileUtils.contentEquals(expectedFile, actualFile));
    }

    private void assertCopyToStoppedContainerWorks(GenericContainer container, String fileNameInContainer, int expectedExitCode) throws IOException {

        int exitCode = container.getDockerClient().waitContainerCmd(container.getContainerId()).start().awaitStatusCode();

        assertEquals("Status code doesn't equals expected code", expectedExitCode, exitCode);

        InspectContainerResponse currentContainerInfo = container.getCurrentContainerInfo();
        assertFalse("The container is no longer running", currentContainerInfo.getState().getRunning());
        assertTrue("The container status is exited", "exited".equals(currentContainerInfo.getState().getStatus()));

        final MountableFile mountableFile = MountableFile.forClasspathResource("test_copy_to_container.txt");
        container.copyFileToContainer(mountableFile, "/test.txt");

        File actualFile = new File(temporaryFolder.getRoot().getAbsolutePath() + "/test_copy_to_container.txt");
        container.copyFileFromContainer("/test.txt", actualFile.getPath());

        File expectedFile = new File(mountableFile.getResolvedPath());
        assertTrue("Files aren't same ", FileUtils.contentEquals(expectedFile, actualFile));
    }
}
