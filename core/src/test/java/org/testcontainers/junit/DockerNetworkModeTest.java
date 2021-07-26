package org.testcontainers.junit;

import com.github.dockerjava.api.model.NetworkSettings;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.startupcheck.OneShotStartupCheckStrategy;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;
import static org.testcontainers.TestImages.TINY_IMAGE;

/**
 * Simple tests of named network modes - more may be possible, but may not be reproducible
 * without other setup steps.
 */
@Slf4j
public class DockerNetworkModeTest {

    @Test
    public void testNoNetworkContainer() {
        try (
            GenericContainer<?> container = new GenericContainer<>(TINY_IMAGE)
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
                .withCommand("true")
                .withNetworkMode("none")
        ) {
            container.start();
            NetworkSettings networkSettings = container.getContainerInfo().getNetworkSettings();

            assertEquals("only one network is set", 1, networkSettings.getNetworks().size());
            assertTrue("network is 'none'", networkSettings.getNetworks().containsKey("none"));
        }
    }

    @Test
    public void testHostNetworkContainer() {
        try (
            GenericContainer<?> container = new GenericContainer<>(TINY_IMAGE)
                .withStartupCheckStrategy(new OneShotStartupCheckStrategy())
                .withCommand("true")
                .withNetworkMode("host")
        ) {
            container.start();
            NetworkSettings networkSettings = container.getContainerInfo().getNetworkSettings();

            assertEquals("only one network is set", 1, networkSettings.getNetworks().size());
            assertTrue("network is 'host'", networkSettings.getNetworks().containsKey("host"));
        }
    }
}
