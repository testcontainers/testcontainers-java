package org.testcontainers.selenium;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;

class ContainerWithoutCapabilitiesTest extends BaseWebDriverContainerTest {

    @AutoClose
    public BrowserWebDriverContainer chrome = new BrowserWebDriverContainer("selenium/standalone-chrome:4.13.0")
        .withNetwork(NETWORK);

    @BeforeEach
    public void setUp() {
        chrome.start();
    }

    @Test
    void chromeIsStartedIfNoCapabilitiesProvided() {
        assertBrowserNameIs(chrome, "chrome", new ChromeOptions());
    }

    @Test
    void simpleExploreTestWhenNoCapabilitiesProvided() {
        doSimpleExplore(chrome, new ChromeOptions());
    }
}
