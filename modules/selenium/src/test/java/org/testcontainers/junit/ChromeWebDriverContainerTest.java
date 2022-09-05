package org.testcontainers.junit;

import org.junit.Before;
import org.junit.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.junit4.Container;

/**
 *
 */
public class ChromeWebDriverContainerTest extends BaseWebDriverContainerTest {

    // junitRule {
    @Container
    public BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>()
        .withCapabilities(new ChromeOptions())
        // }
        .withNetwork(NETWORK);

    @Before
    public void checkBrowserIsIndeedChrome() {
        assertBrowserNameIs(chrome, "chrome", new ChromeOptions());
    }

    @Test
    public void simpleExploreTest() {
        doSimpleExplore(chrome, new ChromeOptions());
    }
}
