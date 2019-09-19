package org.testcontainers.junit;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

public class BrowserWebDriverContainerTest {

    private static final String NO_PROXY_KEY = "no_proxy";

    private static final String NO_PROXY_VALUE = "localhost,.noproxy-domain.com";

    @Test
    public void honorPresetNoProxyEnvironment() {
        try (
            BrowserWebDriverContainer chromeWithNoProxySet = (BrowserWebDriverContainer) new BrowserWebDriverContainer()
                .withCapabilities(new ChromeOptions())
                .withEnv(NO_PROXY_KEY, NO_PROXY_VALUE)
        ) {
            chromeWithNoProxySet.start();

            Object noProxy = chromeWithNoProxySet.getEnvMap().get(NO_PROXY_KEY);
            assertEquals("no_proxy should be preserved by the container rule", NO_PROXY_VALUE, noProxy);
        }
    }

    @Test
    public void provideDefaultNoProxyEnvironmentIfNotSet() {
        try (
            BrowserWebDriverContainer chromeWithoutNoProxySet = new BrowserWebDriverContainer()
                .withCapabilities(new ChromeOptions())

        ) {
            chromeWithoutNoProxySet.start();

            Object noProxy = chromeWithoutNoProxySet.getEnvMap().get(NO_PROXY_KEY);
            assertEquals("no_proxy should be set to default if not already present", "localhost", noProxy);
        }
    }


    @Test
    public void createContainerWithShmVolume() {
        try (
            BrowserWebDriverContainer webDriverContainer = new BrowserWebDriverContainer()
        ) {
            webDriverContainer.start();

            assertEquals("Shm mounts present", webDriverContainer.getContainerInfo().getMounts().size(), 1);
            final InspectContainerResponse.Mount shmMount = webDriverContainer.getContainerInfo().getMounts().get(0);
            assertEquals("Shm mount source is correct", "/dev/shm", shmMount.getSource());
            assertEquals("Shm mount destination is correct", "/dev/shm", shmMount.getDestination().getPath());
            assertEquals("Shm mount mode is correct", shmMount.getMode(), "rw");
        }
    }

    @Test
    public void createContainerWithoutShmVolume() {
        try (
            BrowserWebDriverContainer webDriverContainer = (BrowserWebDriverContainer) new BrowserWebDriverContainer()
             .withSharedMemorySize(512 * FileUtils.ONE_MB)
        ) {
            webDriverContainer.start();
            assertEquals("Shared memory size is configured", 512 * FileUtils.ONE_MB, webDriverContainer.getShmSize());
            assertEquals("No mounts present", webDriverContainer.getContainerInfo().getMounts().size(), 0);
        }
    }

}
