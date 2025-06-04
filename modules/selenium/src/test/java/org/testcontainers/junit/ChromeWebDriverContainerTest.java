package org.testcontainers.junit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
public class ChromeWebDriverContainerTest extends BaseWebDriverContainerTest {

    // junitRule {
    @Container
    public BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>()
        .withCapabilities(new ChromeOptions())
        // }
        .withNetwork(NETWORK);

    @BeforeEach
    public void checkBrowserIsIndeedChrome() {
        assertBrowserNameIs(chrome, "chrome", new ChromeOptions());
    }

    @Test
    public void simpleExploreTest() {
        doSimpleExplore(chrome, new ChromeOptions());
    }
}
