package org.testcontainers.junit;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.utility.DockerImageName;

class SpecificImageNameWebDriverContainerTest extends BaseWebDriverContainerTest {

    private static final DockerImageName FIREFOX_IMAGE = DockerImageName.parse("selenium/standalone-firefox:4.10.0");

    public BrowserWebDriverContainer<?> firefox = new BrowserWebDriverContainer<>(FIREFOX_IMAGE)
        .withCapabilities(new FirefoxOptions())
        .withNetwork(NETWORK);

    @Test
    void simpleExploreTest() {
        firefox.start();
        doSimpleExplore(firefox, new FirefoxOptions());
    }
}
