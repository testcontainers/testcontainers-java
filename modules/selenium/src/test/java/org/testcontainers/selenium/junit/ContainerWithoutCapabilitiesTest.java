package org.testcontainers.selenium.junit;

import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testcontainers.selenium.BrowserWebDriverContainer;

public class ContainerWithoutCapabilitiesTest extends BaseWebDriverContainerTest {

    @Rule
    public BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>().withNetwork(NETWORK);

    @Test
    public void chromeIsStartedIfNoCapabilitiesProvided() {
        assertBrowserNameIs(chrome, "chrome");
    }

    @Test
    public void simpleExploreTestWhenNoCapabilitiesProvided() {
        doSimpleExplore(chrome, new ChromeOptions());
    }
}
