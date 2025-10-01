package org.testcontainers.selenium;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testcontainers.utility.DockerImageName;

class SpecificImageNameWebDriverContainerTest extends BaseWebDriverContainerTest {

    private static final DockerImageName FIREFOX_IMAGE = DockerImageName.parse("selenium/standalone-firefox:4.10.0");

    public BrowserWebDriverContainer firefox = new BrowserWebDriverContainer(FIREFOX_IMAGE).withNetwork(NETWORK);

    @Test
    void simpleExploreTest() {
        firefox.start();
        doSimpleExplore(firefox, new FirefoxOptions());
    }
}
