package org.testcontainers.junit;

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
}
