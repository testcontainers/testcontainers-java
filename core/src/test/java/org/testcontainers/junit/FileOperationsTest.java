package org.testcontainers.junit;

import com.github.dockerjava.api.exception.NotFoundException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.CountingOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.TestImages;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;
import org.testcontainers.utility.MountableFile;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileOperationsTest {

    @TempDir
    public Path temporaryFolder;

    @Test
    void copyFileToContainerFileTest() throws Exception {
        try (
            GenericContainer alpineCopyToContainer = new GenericContainer(TestImages.ALPINE_IMAGE) //
                .withCommand("top")
        ) {
            alpineCopyToContainer.start();
            final MountableFile mountableFile = MountableFile.forClasspathResource("test_copy_to_container.txt");
            alpineCopyToContainer.copyFileToContainer(mountableFile, "/test.txt");

            File actualFile = new File(temporaryFolder.toFile().getAbsolutePath() + "/test_copy_to_container.txt");
            alpineCopyToContainer.copyFileFromContainer("/test.txt", actualFile.getPath());

            File expectedFile = new File(mountableFile.getResolvedPath());
            assertThat(FileUtils.contentEquals(expectedFile, actualFile)).as("Files aren't same ").isTrue();
        }
    }

    @Test
    void copyLargeFilesToContainer() throws Exception {
        File tempFile = temporaryFolder.resolve("large-file").toFile();
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
    void copyFileToContainerFolderTest() throws Exception {
        try (
            GenericContainer alpineCopyToContainer = new GenericContainer(TestImages.ALPINE_IMAGE) //
                .withCommand("top")
        ) {
            alpineCopyToContainer.start();
            final MountableFile mountableFile = MountableFile.forClasspathResource("test_copy_to_container.txt");
            alpineCopyToContainer.copyFileToContainer(mountableFile, "/home/");

            File actualFile = new File(temporaryFolder.toFile().getAbsolutePath() + "/test_copy_to_container.txt");
            alpineCopyToContainer.copyFileFromContainer("/home/test_copy_to_container.txt", actualFile.getPath());

            File expectedFile = new File(mountableFile.getResolvedPath());
            assertThat(FileUtils.contentEquals(expectedFile, actualFile)).as("Files aren't same ").isTrue();
        }
    }

    @Test
    void copyFolderToContainerFolderTest() throws Exception {
        try (
            GenericContainer alpineCopyToContainer = new GenericContainer(TestImages.ALPINE_IMAGE) //
                .withCommand("top")
        ) {
            alpineCopyToContainer.start();
            final MountableFile mountableFile = MountableFile.forClasspathResource("mappable-resource/");
            alpineCopyToContainer.copyFileToContainer(mountableFile, "/home/test/");

            File actualFile = new File(temporaryFolder.toFile().getAbsolutePath() + "/test_copy_to_container.txt");
            alpineCopyToContainer.copyFileFromContainer("/home/test/test-resource.txt", actualFile.getPath());

            File expectedFile = new File(mountableFile.getResolvedPath() + "/test-resource.txt");
            assertThat(FileUtils.contentEquals(expectedFile, actualFile)).as("Files aren't same ").isTrue();
        }
    }

    @Test
    void copyFromContainerShouldFailBecauseNoFileTest() {
        assertThatThrownBy(() -> {
                try (
                    GenericContainer alpineCopyToContainer = new GenericContainer(TestImages.ALPINE_IMAGE) //
                        .withCommand("top")
                ) {
                    alpineCopyToContainer.start();
                    alpineCopyToContainer.copyFileFromContainer(
                        "/home/test.txt",
                        "src/test/resources/copy-from/test.txt"
                    );
                }
            })
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void shouldCopyFileFromContainerTest() throws IOException {
        try (
            GenericContainer alpineCopyToContainer = new GenericContainer(TestImages.ALPINE_IMAGE) //
                .withCommand("top")
        ) {
            alpineCopyToContainer.start();
            final MountableFile mountableFile = MountableFile.forClasspathResource("test_copy_to_container.txt");
            alpineCopyToContainer.copyFileToContainer(mountableFile, "/home/");

            File actualFile = new File(temporaryFolder.toFile().getAbsolutePath() + "/test_copy_from_container.txt");
            alpineCopyToContainer.copyFileFromContainer("/home/test_copy_to_container.txt", actualFile.getPath());

            File expectedFile = new File(mountableFile.getResolvedPath());
            assertThat(FileUtils.contentEquals(expectedFile, actualFile)).as("Files aren't same ").isTrue();
        }
    }

    @Test
    void copyFileOperationsShouldFailWhenNotStartedTest() {
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
    void shouldCopyFileFromExitedContainerTest() {
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
