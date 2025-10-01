package org.testcontainers.selenium;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;

class ChromeWebDriverContainerTest extends BaseWebDriverContainerTest {

    // junitRule {
    public BrowserWebDriverContainer chrome = new BrowserWebDriverContainer("selenium/standalone-chrome:4.13.0")
        // }
        .withNetwork(NETWORK);

    @BeforeEach
    public void checkBrowserIsIndeedChrome() {
        chrome.start();
        assertBrowserNameIs(chrome, "chrome", new ChromeOptions());
    }

    @Test
    void simpleExploreTest() {
        doSimpleExplore(chrome, new ChromeOptions());
    }
}
