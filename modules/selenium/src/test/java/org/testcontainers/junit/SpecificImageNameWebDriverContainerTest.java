package org.testcontainers.junit;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public class SpecificImageNameWebDriverContainerTest extends BaseWebDriverContainerTest {

    private static final DockerImageName FIREFOX_IMAGE = DockerImageName.parse("selenium/standalone-firefox:4.10.0");

    @Container
    public BrowserWebDriverContainer<?> firefox = new BrowserWebDriverContainer<>(FIREFOX_IMAGE)
        .withCapabilities(new FirefoxOptions())
        .withNetwork(NETWORK);

    @Test
    public void simpleExploreTest() {
        doSimpleExplore(firefox, new FirefoxOptions());
    }
}
