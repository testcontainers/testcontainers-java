package org.testcontainers.junit;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;

/**
 *
 */
public class ChromeWebDriverContainerTest extends BaseWebDriverContainerTest {

    // junitRule {
    @Rule
    public BrowserWebDriverContainer chrome = new BrowserWebDriverContainer()
        .withCapabilities(new ChromeOptions());
    // }

    @Before
    public void checkBrowserIsIndeedChrome() {
        assertBrowserNameIs(chrome, "chrome");
    }

    @Test
    public void simpleTest() {
        doSimpleWebdriverTest(chrome);
    }

    @Test
    public void simpleExploreTest() {
        doSimpleExplore(chrome);
    }
}
