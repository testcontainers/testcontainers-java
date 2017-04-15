package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testcontainers.containers.BrowserWebDriverContainer;

import java.io.IOException;
import java.time.Duration;

import static java.time.temporal.ChronoUnit.SECONDS;

/**
 *
 */
public class CustomWaitTimeoutWebDriverContainerTest extends BaseWebDriverContainerTest {

    @Rule
    public BrowserWebDriverContainer chromeWithCustomTimeout = new BrowserWebDriverContainer<>()
            .withDesiredCapabilities(DesiredCapabilities.chrome())
            .withStartupTimeout(Duration.of(30, SECONDS));

    @Test
    public void simpleTest() throws IOException {
        doSimpleWebdriverTest(chromeWithCustomTimeout);
    }
}
