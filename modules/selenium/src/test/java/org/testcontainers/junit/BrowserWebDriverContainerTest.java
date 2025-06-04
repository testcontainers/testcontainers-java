package org.testcontainers.junit;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;

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
            assertThat(noProxy).as("no_proxy should be preserved by the container rule").isEqualTo(NO_PROXY_VALUE);
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
            assertThat(noProxy).as("no_proxy should be set to default if not already present").isEqualTo("localhost");
        }
    }

    @Test
    public void createContainerWithShmVolume() {
        assumeThat(SystemUtils.IS_OS_WINDOWS)
            .as("SHM isn't mounted on Windows")
            .isFalse();
        try (
            BrowserWebDriverContainer webDriverContainer = new BrowserWebDriverContainer()
                .withCapabilities(new FirefoxOptions())
        ) {
            webDriverContainer.start();

            final List<InspectContainerResponse.Mount> shmVolumes = shmVolumes(webDriverContainer);

            assertThat(shmVolumes).as("Only one shm mount present").hasSize(1);
            assertThat(shmVolumes.get(0).getSource()).as("Shm mount source is correct").isEqualTo("/dev/shm");
            assertThat(shmVolumes.get(0).getMode()).as("Shm mount mode is correct").isEqualTo("rw");
        }
    }

    @Test
    public void createContainerWithoutShmVolume() {
        try (
            BrowserWebDriverContainer webDriverContainer = new BrowserWebDriverContainer<>()
                .withSharedMemorySize(512 * FileUtils.ONE_MB)
                .withCapabilities(new FirefoxOptions())
        ) {
            webDriverContainer.start();

            assertThat(webDriverContainer.getShmSize())
                .as("Shared memory size is configured")
                .isEqualTo(512 * FileUtils.ONE_MB);

            assertThat(shmVolumes(webDriverContainer)).as("No shm mounts present").isEqualTo(Collections.emptyList());
        }
    }

    private List<InspectContainerResponse.Mount> shmVolumes(final BrowserWebDriverContainer container) {
        return container
            .getContainerInfo()
            .getMounts()
            .stream()
            // destination path is always /dev/shm
            .filter(m -> m.getDestination().getPath().equals("/dev/shm"))
            .collect(Collectors.toList());
    }
}
