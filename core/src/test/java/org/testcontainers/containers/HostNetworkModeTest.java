package org.testcontainers.containers;

import org.apache.commons.lang3.SystemUtils;
import org.junit.Test;
import org.testcontainers.TestImages;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

public class HostNetworkModeTest {
    private static final String NGINX_IMAGE = "nginx:1.17.10-alpine";

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
//
//    @Test
//    public void givenMacOsOrWindowsShouldThrowIllegalArgumentException() {
//        assumeThat(SystemUtils.IS_OS_MAC || SystemUtils.IS_OS_WINDOWS).isTrue();
//        assertThatThrownBy(() -> {
//            new GenericContainer<>(TestImages.REDIS_IMAGE)
//                .withNetworkMode("host");
//        }).isInstanceOf(IllegalArgumentException.class);
//    }

    @Test
    public void getMappedPortShouldReturnOriginalPort() {
        assumeThat(SystemUtils.IS_OS_LINUX).isTrue();
        try (
            GenericContainer<?> container = new GenericContainer<>(TestImages.REDIS_IMAGE)
                .withNetworkMode("host")
                .withExposedPorts(6379)
        ) {
            container.start();
            assertThat(container.getMappedPort(6379)).isEqualTo(6379);
        }
    }

    @Test
    public void getLivenessCheckPortsShouldReturnExposedPortsWhenHostNetworkMode() {
        assumeThat(SystemUtils.IS_OS_LINUX).isTrue();
        try (
            GenericContainer<?> container = new GenericContainer<>(TestImages.REDIS_IMAGE)
                .withNetworkMode("host")
                .withExposedPorts(6379);
        ) {
            container.start();
            assertThat(container.getLivenessCheckPortNumbers()).containsExactly(6379);
            assertThat(container.getLivenessCheckPort()).isEqualTo(6379);
            assertThat(container.getLivenessCheckPorts()).containsExactly(6379);
        }
    }

    @Test
    public void httpWaitStrategyShouldWorkOnHostNetworkMode() {
        assumeThat(SystemUtils.IS_OS_LINUX).isTrue();
        try (
            GenericContainer<?> container = new GenericContainer<>(NGINX_IMAGE)
                .withNetworkMode("host")
                .withExposedPorts(80)
                .waitingFor(new HttpWaitStrategy().forPort(80))
        ) {
            container.start();
        }
    }
}
