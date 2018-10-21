package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testcontainers.containers.BrowserWebDriverContainer;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

public class BrowserWebDriverContainerTest {

    private static final String NO_PROXY_VALUE = "localhost,.noproxy-domain.com";

    @Rule
    public BrowserWebDriverContainer chromeWithNoProxySet = (BrowserWebDriverContainer) new BrowserWebDriverContainer()
        .withDesiredCapabilities(DesiredCapabilities.chrome())
        .withEnv("no_proxy", NO_PROXY_VALUE);

    @Rule
    public BrowserWebDriverContainer chromeWithoutNoProxySet = new BrowserWebDriverContainer()
        .withDesiredCapabilities(DesiredCapabilities.chrome());

    @Test
    public void honorPresetNoProxyEnvironment() {
        Object no_proxy = chromeWithNoProxySet.getEnvMap().get("no_proxy");
        assertEquals("no_proxy should be preserved by the container rule", NO_PROXY_VALUE, no_proxy);
    }

    @Test
    public void provideDefaultNoProxyEnvironmentIfNotSet() {
        Object no_proxy = chromeWithoutNoProxySet.getEnvMap().get("no_proxy");
        assertEquals("no_proxy should be set to default if not already present", "localhost", no_proxy);
    }
}
