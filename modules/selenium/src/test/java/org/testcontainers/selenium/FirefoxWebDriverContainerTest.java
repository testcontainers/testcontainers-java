package org.testcontainers.selenium;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.firefox.FirefoxOptions;

class FirefoxWebDriverContainerTest extends BaseWebDriverContainerTest {

    // junitRule {
    public BrowserWebDriverContainer firefox = new BrowserWebDriverContainer("selenium/standalone-firefox:4.13.0")
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
