package org.testcontainers.junit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import org.openqa.selenium.chrome.ChromeOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Simple test to check that readiness detection works correctly across major versions of the containers.
 */
@ParameterizedClass(name = "tag: {0}")
@MethodSource("data")
public class SeleniumStartTest {

    public static String[] data() {
        return new String[] { "4.0.0", "3.4.0", "2.53.0" };
    }

    @Parameter(0)
    public String tag;

    @Test
    public void testAdditionalStartupString() {
        final DockerImageName imageName = DockerImageName.parse("selenium/standalone-chrome").withTag(tag);
        try (
            BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>(imageName)
                .withCapabilities(new ChromeOptions())
        ) {
            chrome.start();
        }
    }
}
