package org.testcontainers.junit;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.apache.commons.io.FileUtils;
import org.junit.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;

import java.util.List;

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

            final List<InspectContainerResponse.Mount> mounts = webDriverContainer.getContainerInfo().getMounts();
            assertEquals("Shm mounts present", mounts.size(), 1);

            final InspectContainerResponse.Mount shmMount = mounts.get(0);

            /* source path on Linux/OS X should be /dev/shm
               source path on Windows is likely to be different: /host_mnt/c/dev/shm has been observed, but other paths
               may be possible. As such, be liberal when asserting that the source path is correct.
             */
            assertTrue("Shm mount source is correct", shmMount.getSource().endsWith("/dev/shm"));

            assertEquals("Shm mount destination is correct", "/dev/shm", shmMount.getDestination().getPath());
            assertEquals("Shm mount mode is correct", shmMount.getMode(), "rw");
        }
    }

    @Test
    public void createContainerWithoutShmVolume() {
        try (
            BrowserWebDriverContainer webDriverContainer =  new BrowserWebDriverContainer<>()
             .withSharedMemorySize(512 * FileUtils.ONE_MB)
        ) {
            webDriverContainer.start();
            assertEquals("Shared memory size is configured", 512 * FileUtils.ONE_MB, webDriverContainer.getShmSize());

            final long shmMountCount = webDriverContainer.getContainerInfo().getMounts()
                .stream()
                .filter(m -> "/dev/shm".equals(m.getSource()))
                .count();
            assertEquals("No shm mounts present", shmMountCount, 0L);
        }
    }

}
