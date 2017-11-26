package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testcontainers.containers.BrowserWebDriverContainer;

/**
 *
 */
public class FirefoxWebDriverContainerTest extends BaseWebDriverContainerTest {

    @Rule
    public BrowserWebDriverContainer firefox = new BrowserWebDriverContainer()
            .withDesiredCapabilities(DesiredCapabilities.firefox());

    @Test
    public void simpleTest() {
        doSimpleWebdriverTest(firefox);
    }

    @Test
    public void simpleExploreTest() {
        doSimpleExplore(firefox);
    }
}
