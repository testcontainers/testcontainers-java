package org.testcontainers.junit;

import com.github.dockerjava.api.model.NetworkSettings;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.testcontainers.TestImages;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Simple tests of named network modes - more may be possible, but may not be reproducible
 * without other setup steps.
 */
@Slf4j
public class DockerNetworkModeTest {

    @Test
    public void testNoNetworkContainer() {
        try (
            GenericContainer<?> container = new GenericContainer<>(TestImages.TINY_IMAGE)
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
                .withCommand("true")
                .withNetworkMode("none")
        ) {
            container.start();
            NetworkSettings networkSettings = container.getContainerInfo().getNetworkSettings();

            assertThat(networkSettings.getNetworks()).as("only one network is set").hasSize(1);
            assertThat(networkSettings.getNetworks()).as("network is 'none'").containsKey("none");
        }
    }

    @Test
    public void testHostNetworkContainer() {
        try (
            GenericContainer<?> container = new GenericContainer<>(TestImages.TINY_IMAGE)
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
                .withCommand("true")
                .withNetworkMode("host")
        ) {
            container.start();
            NetworkSettings networkSettings = container.getContainerInfo().getNetworkSettings();

            assertThat(networkSettings.getNetworks()).as("only one network is set").hasSize(1);
            assertThat(networkSettings.getNetworks()).as("network is 'host'").containsKey("host");
        }
    }
}
