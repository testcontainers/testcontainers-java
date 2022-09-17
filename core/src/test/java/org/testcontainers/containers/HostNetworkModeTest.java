package org.testcontainers.containers;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;
import org.testcontainers.TestImages;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

public class HostNetworkModeTest {

    @Test
    public void givenLinuxShouldStart() {
        assumeThat(SystemUtils.IS_OS_LINUX).isTrue();
        try (
            GenericContainer<?> container = new GenericContainer<>(TestImages.REDIS_IMAGE)
                .withNetworkMode("host")
                .withExposedPorts(6379)
        ) {
            container.start();
        }
    }

    @Test
    public void givenMacOsOrWindowsShouldThrowIllegalArgumentException() {
        assumeThat(SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_WINDOWS).isTrue();
        assertThatThrownBy(() -> {
            new GenericContainer<>(TestImages.REDIS_IMAGE)
                .withNetworkMode("host");
        }).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void getMappedPortShouldThrowIllegalArgumentException() {
        assumeThat(SystemUtils.IS_OS_LINUX).isTrue();
        try (
            GenericContainer<?> container = new GenericContainer<>(TestImages.REDIS_IMAGE)
                .withNetworkMode("host")
                .withExposedPorts(6379)
        ) {
            container.start();
            assertThatThrownBy(() -> container.getMappedPort(6379)).isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Test
    public void getLivenessCheckPortsShouldReturnExposedPortsWhenHostNetworkMode() {
        assumeThat(SystemUtils.IS_OS_LINUX).isTrue();
        GenericContainer<?> container = new GenericContainer<>(TestImages.REDIS_IMAGE)
            .withNetworkMode("host")
            .withExposedPorts(6379);
        assertThat(container.getLivenessCheckPortNumbers()).containsExactly(6379);
        assertThat(container.getLivenessCheckPort()).isEqualTo(6379);
        assertThat(container.getLivenessCheckPorts()).containsExactly(6379);
    }
}
