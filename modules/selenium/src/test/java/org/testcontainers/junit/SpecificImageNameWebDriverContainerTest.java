package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.utility.DockerImageName;

public class SpecificImageNameWebDriverContainerTest extends BaseWebDriverContainerTest {

    private static final DockerImageName FIREFOX_IMAGE = DockerImageName
        .parse("selenium/standalone-firefox:2.53.1-beryllium");

    @Rule
    public BrowserWebDriverContainer<?> firefox = new BrowserWebDriverContainer<>(FIREFOX_IMAGE)
        .withCapabilities(new FirefoxOptions())
        .withNetwork(NETWORK);

    @Test
    public void simpleExploreTest() {
        doSimpleExplore(firefox);
    }
}
