/*
 * MIT License
 *
 * Copyright (c) 2021-present HiveMQ GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
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
