package org.testcontainers.junit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;

class ChromeWebDriverContainerTest extends BaseWebDriverContainerTest {

    // junitRule {
    public BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>()
        .withCapabilities(new ChromeOptions())
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
