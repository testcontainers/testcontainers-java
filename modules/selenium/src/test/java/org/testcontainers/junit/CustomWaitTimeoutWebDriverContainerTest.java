package org.testcontainers.junit;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

class CustomWaitTimeoutWebDriverContainerTest extends BaseWebDriverContainerTest {

    public BrowserWebDriverContainer<?> chromeWithCustomTimeout = new BrowserWebDriverContainer<>()
        .withCapabilities(new ChromeOptions())
        .withStartupTimeout(Duration.of(30, ChronoUnit.SECONDS))
        .withNetwork(NETWORK);

    @Test
    void simpleExploreTest() {
        chromeWithCustomTimeout.start();
        doSimpleExplore(chromeWithCustomTimeout, new ChromeOptions());
    }
}
