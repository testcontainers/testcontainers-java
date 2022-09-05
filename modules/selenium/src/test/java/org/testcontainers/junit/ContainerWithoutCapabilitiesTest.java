package org.testcontainers.junit;

import org.junit.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.junit4.Container;

public class ContainerWithoutCapabilitiesTest extends BaseWebDriverContainerTest {

    @Container
    public BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>().withNetwork(NETWORK);

    @Test
    public void chromeIsStartedIfNoCapabilitiesProvided() {
        assertBrowserNameIs(chrome, "chrome", new ChromeOptions());
    }

    @Test
    public void simpleExploreTestWhenNoCapabilitiesProvided() {
        doSimpleExplore(chrome, new ChromeOptions());
    }
}
