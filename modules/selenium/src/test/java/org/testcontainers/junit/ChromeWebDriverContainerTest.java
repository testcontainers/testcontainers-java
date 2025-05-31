package org.testcontainers.junit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.junit4.TestcontainersRule;

/**
 *
 */
public class ChromeWebDriverContainerTest extends BaseWebDriverContainerTest {

    // junitRule {
    @Rule
    public TestcontainersRule<BrowserWebDriverContainer<?>> chrome = new TestcontainersRule<>(
        new BrowserWebDriverContainer<>()
            .withCapabilities(new ChromeOptions())
            // }
            .withNetwork(NETWORK)
    );

    @Before
    public void checkBrowserIsIndeedChrome() {
        assertBrowserNameIs(chrome.get(), "chrome", new ChromeOptions());
    }

    @Test
    public void simpleExploreTest() {
        doSimpleExplore(chrome.get(), new ChromeOptions());
    }
}
