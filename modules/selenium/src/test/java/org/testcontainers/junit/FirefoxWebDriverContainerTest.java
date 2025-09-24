package org.testcontainers.junit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;

class FirefoxWebDriverContainerTest extends BaseWebDriverContainerTest {

    // junitRule {
    public BrowserWebDriverContainer<?> firefox = new BrowserWebDriverContainer<>()
        .withCapabilities(new FirefoxOptions())
        // }
        .withNetwork(NETWORK);

    @BeforeEach
    public void checkBrowserIsIndeedFirefox() {
        firefox.start();
        assertBrowserNameIs(firefox, "firefox", new FirefoxOptions());
    }

    @Test
    void simpleExploreTest() {
        doSimpleExplore(firefox, new FirefoxOptions());
    }
}
