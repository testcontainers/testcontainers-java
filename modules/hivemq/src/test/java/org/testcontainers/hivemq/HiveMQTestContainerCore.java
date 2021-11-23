/*
 * Copyright 2020 HiveMQ and the HiveMQ Community
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.testcontainers.hivemq;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Yannick Weber
 */
class HiveMQTestContainerCore {

    final @NotNull HiveMQContainer container = new HiveMQContainer();

    @TempDir
    File tempDir;

    @Test
    void withExtension_fileDoesNotExist_Exception() {
        final MountableFile mountableFile = MountableFile.forHostPath("/this/does/not/exist");
        assertThrows(ContainerLaunchException.class, () -> container.withExtension(mountableFile));
    }

    @Test
    void withExtension_fileNoDirectory_Exception() throws IOException {
        final File extension = new File(tempDir, "extension");
        assertTrue(extension.createNewFile());
        final MountableFile mountableFile = MountableFile.forHostPath(extension.getAbsolutePath());
        assertThrows(ContainerLaunchException.class, () -> container.withExtension(mountableFile));
    }

    @Test
    void withLicense_fileDoesNotExist_Exception() {
        final MountableFile mountableFile = MountableFile.forHostPath("/this/does/not/exist");
        assertThrows(ContainerLaunchException.class, () -> container.withLicense(mountableFile));
    }

    @Test
    void withExtension_fileEndingWrong_Exception() throws IOException {
        final File extension = new File(tempDir, "extension.wrong");
        assertTrue(extension.createNewFile());
        final MountableFile mountableFile = MountableFile.forHostPath(extension.getAbsolutePath());
        assertThrows(ContainerLaunchException.class, () -> container.withLicense(mountableFile));
    }

    @Test
    void withHiveMQConfig_fileDoesNotExist_Exception() {
        final MountableFile mountableFile = MountableFile.forHostPath("/this/does/not/exist");
        assertThrows(ContainerLaunchException.class, () -> container.withHiveMQConfig(mountableFile));
    }

    @Test
    void withFileInHomeFolder_fileDoesNotExist_Exception() {
        final MountableFile mountableFile = MountableFile.forHostPath("/this/does/not/exist");
        assertThrows(ContainerLaunchException.class, () -> container.withFileInHomeFolder(mountableFile, "some/path"));
    }

    @Test
    void withFileInHomeFolder_pathEmpty_Exception() {
        final MountableFile mountableFile = MountableFile.forHostPath("/this/does/not/exist");
        assertThrows(ContainerLaunchException.class, () -> container.withFileInHomeFolder(mountableFile, ""));
    }

    @Test
    void withFileInExtensionHomeFolder_withPath_fileDoesNotExist_Exception() {
        final MountableFile mountableFile = MountableFile.forHostPath("/this/does/not/exist");
        assertThrows(ContainerLaunchException.class, () -> container.withFileInExtensionHomeFolder(mountableFile, "my-extension", "some/path"));
    }

    @Test
    void withFileInExtensionHomeFolder_fileDoesNotExist_Exception() {
        final MountableFile mountableFile = MountableFile.forHostPath("/this/does/not/exist");
        assertThrows(ContainerLaunchException.class, () -> container.withFileInExtensionHomeFolder(mountableFile, "my-extension"));
    }
}
