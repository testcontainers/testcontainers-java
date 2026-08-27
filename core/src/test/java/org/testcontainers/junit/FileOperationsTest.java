package org.testcontainers.junit;

import com.github.dockerjava.api.exception.NotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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

    @Test
    public void copyPathFromContainer_singleFileTest() throws IOException {
        try (
              GenericContainer container = new GenericContainer(TestImages.ALPINE_IMAGE) //
                    .withCommand("top")
        ) {
            container.start();
            final MountableFile mountableFile = MountableFile.forClasspathResource("test_copy_to_container.txt");
            container.copyFileToContainer(mountableFile, "/home/");

            File actualFile = new File(temporaryFolder.getRoot().getAbsolutePath() + "/test_copy_from_container.txt");
            container.copyPathFromContainer("/home/test_copy_to_container.txt", actualFile.getPath());

            File expectedFile = new File(mountableFile.getResolvedPath());
            assertThat(FileUtils.contentEquals(expectedFile, actualFile)).as("Files aren't same ").isTrue();
        }
    }

    @Test
    public void copyPathFromContainer_nestedDirectoryStructureWithoutFilesTest()
          throws IOException, InterruptedException {
        try (GenericContainer<?> container = new GenericContainer<>(TestImages.ALPINE_IMAGE)
              .withCommand("sleep", "infinity")) {
            container.start();

            // create directory structure
            container.execInContainer("mkdir", "-p", "/home/nested/directoryOne/inner");
            container.execInContainer("mkdir", "-p", "/home/nested/directoryTwo/inner");

            File hostPath = temporaryFolder.getRoot();
            container.copyPathFromContainer("/home", hostPath.getPath());

            // assert layer 1 structure
            assertDirectoryContents(hostPath.toPath(), "nested");

            // assert layer 2 structure (nested/)
            assertDirectoryContents(Paths.get(hostPath.getPath(), "nested"), "directoryOne", "directoryTwo");

            // assert layer 3 structure (nested/directoryOne/ and nested/directoryTwo/)
            assertDirectoryContents(Paths.get(hostPath.getPath(), "nested", "directoryOne"), "inner");
            assertDirectoryContents(Paths.get(hostPath.getPath(), "nested", "directoryTwo"), "inner");
        }
    }

    @Test
    public void copyPathFromContainer_filesInNestedDirectory()
          throws IOException, InterruptedException {
        try (GenericContainer<?> container = new GenericContainer<>(TestImages.ALPINE_IMAGE)
              .withCommand("sleep", "infinity")) {
            container.start();

            // create directory and files
            container.execInContainer("mkdir", "-p", "/home/test/nested");
            container.execInContainer("sh","-c", "echo 'rootfile' > /home/test/root.txt ");
            container.execInContainer("sh","-c", "echo 'nestedfile' > /home/test/nested/nested.txt");

            Path actualRootPath = temporaryFolder.getRoot().toPath();
            container.copyPathFromContainer("/home/test", actualRootPath.toString());

            // assert correct file structure
            assertDirectoryContents(actualRootPath, "nested", "root.txt");
            assertDirectoryContents(actualRootPath.resolve("nested"), "nested.txt");

            // assert correct file content
            assertThat(actualRootPath.resolve("root.txt"))
                  .hasContent("rootfile");
            assertThat(actualRootPath.resolve("nested/nested.txt"))
                  .hasContent("nestedfile");
        }
    }

    @Test(expected = NotFoundException.class)
    public void copyPathFromContainer_nonExistingDirectory_shouldFail()
          throws IOException, InterruptedException {
        try (GenericContainer<?> container = new GenericContainer<>(TestImages.ALPINE_IMAGE)
              .withCommand("sleep", "infinity")) {
            container.start();

            Path actualRootPath = temporaryFolder.getRoot().toPath();
            container.copyPathFromContainer("/home/test", actualRootPath.toString());
        }
    }

    @Test
    public void copyPathFromContainer_conflictingHostFile_shouldFail()
          throws IOException, InterruptedException {
        try (GenericContainer<?> container = new GenericContainer<>(TestImages.ALPINE_IMAGE)
              .withCommand("sleep", "infinity")) {
            container.start();

            container.execInContainer("mkdir", "-p", "/home/test/conflict/dir");

            Path actualRootPath = temporaryFolder.getRoot().toPath();
            // create file `conflict` ... will conflict the copied directory `conflict/dir`
            FileUtils.touch(actualRootPath.resolve("conflict").toFile());

            assertThatThrownBy(() -> {
                container.copyPathFromContainer("/home/test", actualRootPath.toString());
            })
                  .isInstanceOf(IOException.class)
                  .hasMessageContaining("copyPathFromContainer cannot create directory")
                  .hasMessageContaining("as a file at this path already exists.");
        }
    }

    private void assertDirectoryContents(Path directory, String... expectedContents) {
        assertThat(directory.toFile().list()).containsExactlyInAnyOrder(expectedContents);
    }

}
