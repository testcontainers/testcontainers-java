package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.junit4.TestcontainersRule;
import org.testcontainers.utility.DockerImageName;

public class SpecificImageNameWebDriverContainerTest extends BaseWebDriverContainerTest {

    private static final DockerImageName FIREFOX_IMAGE = DockerImageName.parse("selenium/standalone-firefox:4.10.0");

    @Rule
    public TestcontainersRule<BrowserWebDriverContainer<?>> firefox = new TestcontainersRule<>(
        new BrowserWebDriverContainer<>(FIREFOX_IMAGE).withCapabilities(new FirefoxOptions()).withNetwork(NETWORK)
    );

    @Test
    public void simpleExploreTest() {
        doSimpleExplore(firefox.get(), new FirefoxOptions());
    }
}
