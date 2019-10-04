package org.testcontainers.junit;

import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

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

            assertEquals("Only one shm mount present", countCorrectlyConfiguredShmVolumes(webDriverContainer), 1L);
            assertEquals("No additional shm mounts present", countAnyShmVolumes(webDriverContainer), 1L);
        }
    }

    @Test
    public void createContainerWithoutShmVolume() {
        try (
            BrowserWebDriverContainer webDriverContainer = new BrowserWebDriverContainer<>()
                .withSharedMemorySize(512 * FileUtils.ONE_MB)
        ) {
            webDriverContainer.start();

            assertEquals("Shared memory size is configured",
                512 * FileUtils.ONE_MB,
                webDriverContainer.getShmSize());

            assertEquals("No shm mounts present", countAnyShmVolumes(webDriverContainer), 0L);
        }
    }

    private long countAnyShmVolumes(final BrowserWebDriverContainer container) {
        return container.getContainerInfo().getMounts()
            .stream()
            // destination path is always /dev/shm
            .filter(m -> m.getDestination().getPath().equals("/dev/shm"))
            .count();
    }

    private long countCorrectlyConfiguredShmVolumes(final BrowserWebDriverContainer container) {
        return container.getContainerInfo().getMounts()
            .stream()
            /* source path on Linux/OS X should be /dev/shm
               source path on Windows is likely to be different: /host_mnt/c/dev/shm has been observed, but other paths
               may be possible. As such, be liberal when checking that the source path is correct.
             */
            .filter(m -> m.getSource().endsWith("/dev/shm"))
            // destination path is always /dev/shm
            .filter(m -> m.getDestination().getPath().equals("/dev/shm"))
            // mode should always be r/w
            .filter(m -> m.getMode().equals("rw"))
            .count();
    }
}
