package org.testcontainers.junit5;

import org.openqa.selenium.chrome.ChromeOptions;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class BaseTest {

    @Container
    public static final BrowserWebDriverContainer<?> container = new BrowserWebDriverContainer<>("selenium/standalone-chrome:2.45.0")
        .withCapabilities(new ChromeOptions());

}
