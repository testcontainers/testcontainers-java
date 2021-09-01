package org.testcontainers.providers.kubernetes.mounts;


import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.controller.model.BindMode;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class CopyContentsHostMountStrategyTest {

    private static final String TEST_IMAGE = "nginx:1.20.1-alpine";
    private static final String TEST_HOST_DIR = new File("src/test/resources").toPath().toAbsolutePath().toString();
    private static final String TEST_CONTAINER_DIR_MOUNT = "/mnt/data";

    private static final String TEST_HOST_FILE = new File("src/test/resources/test.txt").toPath().toAbsolutePath().toString();
    private static final String TEST_CONTAINER_FILE_MOUNT = "/mnt/data/test.txt";

    @Test
    public void directoryMountsArePresent() throws IOException, InterruptedException {
        try (GenericContainer<?> container = new GenericContainer<>(TEST_IMAGE)) {
            container.addFileSystemBind(TEST_HOST_DIR, TEST_CONTAINER_DIR_MOUNT, BindMode.READ_WRITE);
            container.start();

            inspectMount(container);
        }
    }

    @Test
    public void fileMountsArePresent() throws IOException, InterruptedException {
        try (GenericContainer<?> container = new GenericContainer<>(TEST_IMAGE)) {
            container.addFileSystemBind(TEST_HOST_FILE, TEST_CONTAINER_FILE_MOUNT, BindMode.READ_WRITE);
            container.start();

            inspectMount(container);
        }
    }

    private void inspectMount(GenericContainer<?> container) throws IOException, InterruptedException {
        assertThat(container.execInContainer("ls", TEST_CONTAINER_DIR_MOUNT).getExitCode())
            .withFailMessage("The directory should be present")
            .isZero();

        assertThat(container.execInContainer("cat", TEST_CONTAINER_FILE_MOUNT).getExitCode())
            .withFailMessage("The file should be readable")
            .isZero();

    }

}
