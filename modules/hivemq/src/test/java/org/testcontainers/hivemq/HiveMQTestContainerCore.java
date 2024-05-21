package org.testcontainers.hivemq;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class HiveMQTestContainerCore {

    @NotNull
    final HiveMQContainer container = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq-ce").withTag("2024.3"));

    @TempDir
    File tempDir;

    @Test
    void withExtension_fileDoesNotExist_Exception() {
        final MountableFile mountableFile = MountableFile.forHostPath("/this/does/not/exist");
        assertThatExceptionOfType(ContainerLaunchException.class)
            .isThrownBy(() -> container.withExtension(mountableFile));
    }

    @Test
    void withExtension_fileNoDirectory_Exception() throws IOException {
        final File extension = new File(tempDir, "extension");
        assertThat(extension.createNewFile()).isTrue();
        final MountableFile mountableFile = MountableFile.forHostPath(extension.getAbsolutePath());
        assertThatExceptionOfType(ContainerLaunchException.class)
            .isThrownBy(() -> container.withExtension(mountableFile));
    }

    @Test
    void withLicense_fileDoesNotExist_Exception() {
        final MountableFile mountableFile = MountableFile.forHostPath("/this/does/not/exist");
        assertThatExceptionOfType(ContainerLaunchException.class)
            .isThrownBy(() -> container.withLicense(mountableFile));
    }

    @Test
    void withExtension_fileEndingWrong_Exception() throws IOException {
        final File extension = new File(tempDir, "extension.wrong");
        assertThat(extension.createNewFile()).isTrue();
        final MountableFile mountableFile = MountableFile.forHostPath(extension.getAbsolutePath());
        assertThatExceptionOfType(ContainerLaunchException.class)
            .isThrownBy(() -> container.withLicense(mountableFile));
    }

    @Test
    void withHiveMQConfig_fileDoesNotExist_Exception() {
        final MountableFile mountableFile = MountableFile.forHostPath("/this/does/not/exist");
        assertThatExceptionOfType(ContainerLaunchException.class)
            .isThrownBy(() -> container.withHiveMQConfig(mountableFile));
    }

    @Test
    void withFileInHomeFolder_fileDoesNotExist_Exception() {
        final MountableFile mountableFile = MountableFile.forHostPath("/this/does/not/exist");
        assertThatExceptionOfType(ContainerLaunchException.class)
            .isThrownBy(() -> container.withFileInHomeFolder(mountableFile, "some/path"));
    }

    @Test
    void withFileInHomeFolder_pathEmpty_Exception() {
        final MountableFile mountableFile = MountableFile.forHostPath("/this/does/not/exist");
        assertThatExceptionOfType(ContainerLaunchException.class)
            .isThrownBy(() -> container.withFileInHomeFolder(mountableFile, ""));
    }

    @Test
    void withFileInExtensionHomeFolder_withPath_fileDoesNotExist_Exception() {
        final MountableFile mountableFile = MountableFile.forHostPath("/this/does/not/exist");
        assertThatExceptionOfType(ContainerLaunchException.class)
            .isThrownBy(() -> container.withFileInExtensionHomeFolder(mountableFile, "my-extension", "some/path"));
    }

    @Test
    void withFileInExtensionHomeFolder_fileDoesNotExist_Exception() {
        final MountableFile mountableFile = MountableFile.forHostPath("/this/does/not/exist");
        assertThatExceptionOfType(ContainerLaunchException.class)
            .isThrownBy(() -> {
                final HiveMQContainer hiveMQContainer = container.withFileInExtensionHomeFolder(
                    mountableFile,
                    "my-extension"
                );
            });
    }
}
