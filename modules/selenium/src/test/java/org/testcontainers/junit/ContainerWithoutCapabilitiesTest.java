package org.testcontainers.junit;

import org.junit.jupiter.api.AutoClose;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;

class ContainerWithoutCapabilitiesTest extends BaseWebDriverContainerTest {

    @AutoClose
    public BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>().withNetwork(NETWORK);

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
