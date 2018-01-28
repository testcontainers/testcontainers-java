package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testcontainers.containers.BrowserWebDriverContainer;

import java.io.IOException;

/**
 *
 */
public class SpecificImageNameWebDriverContainerTest extends BaseWebDriverContainerTest {

    @Rule
    public BrowserWebDriverContainer firefox = new BrowserWebDriverContainer("selenium/standalone-firefox:2.53.1-beryllium")
            .withDesiredCapabilities(DesiredCapabilities.firefox());

    @Test
    public void simpleTest() throws IOException {
        doSimpleWebdriverTest(firefox);
    }

    @Test
    public void simpleExploreTest() throws IOException {
        doSimpleExplore(firefox);
    }
}
