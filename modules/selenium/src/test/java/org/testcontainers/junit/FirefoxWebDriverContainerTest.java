package org.testcontainers.junit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.junit4.TestcontainersRule;

/**
 *
 */
public class FirefoxWebDriverContainerTest extends BaseWebDriverContainerTest {

    // junitRule {
    @Rule
    public TestcontainersRule<BrowserWebDriverContainer<?>> firefox = new TestcontainersRule<>(
        new BrowserWebDriverContainer<>()
            .withCapabilities(new FirefoxOptions())
            // }
            .withNetwork(NETWORK)
    );

    @Before
    public void checkBrowserIsIndeedFirefox() {
        assertBrowserNameIs(firefox.get(), "firefox", new FirefoxOptions());
    }

    @Test
    public void simpleExploreTest() {
        doSimpleExplore(firefox.get(), new FirefoxOptions());
    }
}
