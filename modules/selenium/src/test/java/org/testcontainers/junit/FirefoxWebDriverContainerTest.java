package org.testcontainers.junit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class FirefoxWebDriverContainerTest extends BaseWebDriverContainerTest {

    // junitRule {
    @Container
    public BrowserWebDriverContainer<?> firefox = new BrowserWebDriverContainer<>()
        .withCapabilities(new FirefoxOptions())
        // }
        .withNetwork(NETWORK);

    @BeforeEach
    public void checkBrowserIsIndeedFirefox() {
        assertBrowserNameIs(firefox, "firefox", new FirefoxOptions());
    }

    @Test
    public void simpleExploreTest() {
        doSimpleExplore(firefox, new FirefoxOptions());
    }
}
