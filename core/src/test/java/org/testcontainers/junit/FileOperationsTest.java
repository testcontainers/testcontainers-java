package org.testcontainers.junit;

import com.github.dockerjava.api.exception.NotFoundException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.testcontainers.TestImages;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.utility.MountableFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class FileOperationsTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void copyFileToContainerFileTest() throws Exception {
        try (
            GenericContainer alpineCopyToContainer = new GenericContainer(TestImages.ALPINE_IMAGE) //
                .withCommand("top")
        ) {
            alpineCopyToContainer.start();
            final MountableFile mountableFile = MountableFile.forClasspathResource("test_copy_to_container.txt");
            alpineCopyToContainer.copyFileToContainer(mountableFile, "/test.txt");

            File actualFile = new File(temporaryFolder.getRoot().getAbsolutePath() + "/test_copy_to_container.txt");
            alpineCopyToContainer.copyFileFromContainer("/test.txt", actualFile.getPath());

            File expectedFile = new File(mountableFile.getResolvedPath());
            assertThat(FileUtils.contentEquals(expectedFile, actualFile)).as("Files aren't same ").isTrue();
        }
    }

    @Test
    public void copyLargeFilesToContainer() throws Exception {
        File tempFile = temporaryFolder.newFile();
        try (
            GenericContainer<?> alpineCopyToContainer = new GenericContainer<>(TestImages.ALPINE_IMAGE) //
                .withCommand("sleep", "infinity")
        ) {
            alpineCopyToContainer.start();
            final long byteCount;
            try (
                FileOutputStream fos = new FileOutputStream(tempFile);
                CountingOutputStream cos = new CountingOutputStream(fos);
                BufferedOutputStream bos = new BufferedOutputStream(cos)
            ) {
                for (int i = 0; i < 0x4000; i++) {
                    byte[] bytes = new byte[0xFFFF];
                    bos.write(bytes);
                }
                bos.flush();
                byteCount = cos.getByteCount();
            }
            final MountableFile mountableFile = MountableFile.forHostPath(tempFile.getPath());
            final String containerPath = "/test.bin";
            alpineCopyToContainer.copyFileToContainer(mountableFile, containerPath);

            final Container.ExecResult execResult = alpineCopyToContainer.execInContainer( //
                "stat",
                "-c",
                "%s",
                containerPath
            );
            assertThat(execResult.getStdout()).isEqualToIgnoringNewLines(Long.toString(byteCount));
        } finally {
            tempFile.delete();
        }
    }

    @Test
    public void copyFileToContainerFolderTest() throws Exception {
        try (
            GenericContainer alpineCopyToContainer = new GenericContainer(TestImages.ALPINE_IMAGE) //
                .withCommand("top")
        ) {
            alpineCopyToContainer.start();
            final MountableFile mountableFile = MountableFile.forClasspathResource("test_copy_to_container.txt");
            alpineCopyToContainer.copyFileToContainer(mountableFile, "/home/");

            File actualFile = new File(temporaryFolder.getRoot().getAbsolutePath() + "/test_copy_to_container.txt");
            alpineCopyToContainer.copyFileFromContainer("/home/test_copy_to_container.txt", actualFile.getPath());

            File expectedFile = new File(mountableFile.getResolvedPath());
            assertThat(FileUtils.contentEquals(expectedFile, actualFile)).as("Files aren't same ").isTrue();
        }
    }

    @Test
    public void copyFolderToContainerFolderTest() throws Exception {
        try (
            GenericContainer alpineCopyToContainer = new GenericContainer(TestImages.ALPINE_IMAGE) //
                .withCommand("top")
        ) {
            alpineCopyToContainer.start();
            final MountableFile mountableFile = MountableFile.forClasspathResource("mappable-resource/");
            alpineCopyToContainer.copyFileToContainer(mountableFile, "/home/test/");

            File actualFile = new File(temporaryFolder.getRoot().getAbsolutePath() + "/test_copy_to_container.txt");
            alpineCopyToContainer.copyFileFromContainer("/home/test/test-resource.txt", actualFile.getPath());

            File expectedFile = new File(mountableFile.getResolvedPath() + "/test-resource.txt");
            assertThat(FileUtils.contentEquals(expectedFile, actualFile)).as("Files aren't same ").isTrue();
        }
    }

    @Test(expected = NotFoundException.class)
    public void copyFromContainerShouldFailBecauseNoFileTest() throws NotFoundException {
        try (
            GenericContainer alpineCopyToContainer = new GenericContainer(TestImages.ALPINE_IMAGE) //
                .withCommand("top")
        ) {
            alpineCopyToContainer.start();
            alpineCopyToContainer.copyFileFromContainer("/home/test.txt", "src/test/resources/copy-from/test.txt");
        }
    }

    @Test
    public void shouldCopyFileFromContainerTest() throws IOException {
        try (
            GenericContainer alpineCopyToContainer = new GenericContainer(TestImages.ALPINE_IMAGE) //
                .withCommand("top")
        ) {
            alpineCopyToContainer.start();
            final MountableFile mountableFile = MountableFile.forClasspathResource("test_copy_to_container.txt");
            alpineCopyToContainer.copyFileToContainer(mountableFile, "/home/");

            File actualFile = new File(temporaryFolder.getRoot().getAbsolutePath() + "/test_copy_from_container.txt");
            alpineCopyToContainer.copyFileFromContainer("/home/test_copy_to_container.txt", actualFile.getPath());

            File expectedFile = new File(mountableFile.getResolvedPath());
            assertThat(FileUtils.contentEquals(expectedFile, actualFile)).as("Files aren't same ").isTrue();
        }
    }

    @Test
    public void copyFileOperationsShouldFailWhenNotStartedTest() {
        try (GenericContainer<?> container = new GenericContainer<>(TestImages.ALPINE_IMAGE).withCommand("top")) {
            assertThatThrownBy(() -> {
                    MountableFile mountableFile = MountableFile.forClasspathResource("test_copy_to_container.txt");
                    container.copyFileToContainer(mountableFile, "/home/test.txt");
                })
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("can only be used with created / running container");

            assertThatThrownBy(() -> {
                    container.copyFileFromContainer("/home/test_copy_to_container.txt", IOUtils::toByteArray);
                })
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("can only be used when the Container is created");
        }
    }

    @Test
    public void shouldCopyFileFromExitedContainerTest() throws IOException {
        try (
            GenericContainer<?> container = new GenericContainer<>(TestImages.ALPINE_IMAGE)
                .withCommand("sh", "-c", "echo -n 'Hello!' > /home/file_in_container.txt")
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
        ) {
            container.start();
            assertThat(
                container.getDockerClient().waitContainerCmd(container.getContainerId()).start().awaitStatusCode()
            )
                .isZero();

            container.copyFileFromContainer("/home/file_in_container.txt", IOUtils::toByteArray);

            container.copyFileToContainer(
                MountableFile.forClasspathResource("test_copy_to_container.txt"),
                "/test.txt"
            );
        }
    }
}
