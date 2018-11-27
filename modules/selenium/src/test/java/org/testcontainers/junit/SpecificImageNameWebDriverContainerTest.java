package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;

/**
 *
 */
public class SpecificImageNameWebDriverContainerTest extends BaseWebDriverContainerTest {

    @Rule
    public BrowserWebDriverContainer firefox = new BrowserWebDriverContainer("selenium/standalone-firefox:2.53.1-beryllium")
        .withCapabilities(new FirefoxOptions());

    @Test
    public void simpleTest() {
        doSimpleWebdriverTest(firefox);
    }

    @Test
    public void simpleExploreTest() {
        doSimpleExplore(firefox);
    }
}
