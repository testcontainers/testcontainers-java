package org.testcontainers.junit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.testcontainers.containers.BrowserWebDriverContainer;

/**
 * Simple test to check that readiness detection works correctly across major versions of the containers.
 */
@RunWith(Parameterized.class)
public class Selenium3xTest {

    @Parameterized.Parameters(name = "tag: {0}")
    public static String[] data() {
        return new String[]{"3.4.0", "2.53.0", "2.45.0"};
    }

    @Parameterized.Parameter()
    public String tag;

    @Test
    public void testAdditionalStartupString() {
        try (BrowserWebDriverContainer chrome = new BrowserWebDriverContainer("selenium/standalone-chrome-debug:" + tag)
                .withDesiredCapabilities(DesiredCapabilities.chrome())) {
            chrome.start();
        }
    }
}
