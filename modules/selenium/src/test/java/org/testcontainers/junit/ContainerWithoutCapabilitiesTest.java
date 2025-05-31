package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.junit4.TestcontainersRule;

public class ContainerWithoutCapabilitiesTest extends BaseWebDriverContainerTest {

    @Rule
    public TestcontainersRule<BrowserWebDriverContainer<?>> chrome = new TestcontainersRule<>(
        new BrowserWebDriverContainer<>().withNetwork(NETWORK)
    );

    @Test
    public void chromeIsStartedIfNoCapabilitiesProvided() {
        assertBrowserNameIs(chrome.get(), "chrome", new ChromeOptions());
    }

    @Test
    public void simpleExploreTestWhenNoCapabilitiesProvided() {
        doSimpleExplore(chrome.get(), new ChromeOptions());
    }
}
