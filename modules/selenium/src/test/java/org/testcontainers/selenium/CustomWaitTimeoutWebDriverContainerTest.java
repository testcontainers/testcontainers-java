package org.testcontainers.selenium;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

class CustomWaitTimeoutWebDriverContainerTest extends BaseWebDriverContainerTest {

    public BrowserWebDriverContainer chromeWithCustomTimeout = new BrowserWebDriverContainer(
        "selenium/standalone-chrome:4.13.0"
    )
        .withStartupTimeout(Duration.of(30, ChronoUnit.SECONDS))
        .withNetwork(NETWORK);

    @Test
    void simpleExploreTest() {
        chromeWithCustomTimeout.start();
        doSimpleExplore(chromeWithCustomTimeout, new ChromeOptions());
    }
}
