package org.testcontainers.junit;

import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.junit4.Container;

/**
 *
 */
public class FirefoxWebDriverContainerTest extends BaseWebDriverContainerTest {

    // junitRule {
    @Container
    public BrowserWebDriverContainer<?> firefox = new BrowserWebDriverContainer<>()
        .withCapabilities(new FirefoxOptions())
        // }
        .withNetwork(NETWORK);

    @Before
    public void checkBrowserIsIndeedFirefox() {
        assertBrowserNameIs(firefox, "firefox", new FirefoxOptions());
    }

    @Test
    public void simpleExploreTest() {
        doSimpleExplore(firefox, new FirefoxOptions());
    }
}
