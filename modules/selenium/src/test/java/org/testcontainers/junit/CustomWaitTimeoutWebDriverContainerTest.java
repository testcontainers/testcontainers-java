package org.testcontainers.junit;

import org.junit.jupiter.api.Test;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Testcontainers
public class CustomWaitTimeoutWebDriverContainerTest extends BaseWebDriverContainerTest {

    @Container
    public BrowserWebDriverContainer<?> chromeWithCustomTimeout = new BrowserWebDriverContainer<>()
        .withCapabilities(new ChromeOptions())
        .withStartupTimeout(Duration.of(30, ChronoUnit.SECONDS))
        .withNetwork(NETWORK);

    @Test
    public void simpleExploreTest() {
        doSimpleExplore(chromeWithCustomTimeout, new ChromeOptions());
    }
}
