package org.testcontainers.junit;

import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.containers.BrowserWebDriverContainer;

import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

/**
 *
 */
public class BaseWebDriverContainerTest {

    protected void doSimpleWebdriverTest(BrowserWebDriverContainer rule) {
        RemoteWebDriver driver = setupDriverFromRule(rule);
        System.out.println("Selenium remote URL is: " + rule.getSeleniumAddress());
        System.out.println("VNC URL is: " + rule.getVncAddress());

        driver.get("http://www.google.com");
        WebElement search = driver.findElement(By.name("q"));
        search.sendKeys("testcontainers");
        search.submit();
        assertEquals("the word 'testcontainers' appears in the search box", "testcontainers",
            search.getAttribute("value"));
    }

    protected void assertBrowserNameIs(BrowserWebDriverContainer rule, String expectedName) {
        RemoteWebDriver driver = setupDriverFromRule(rule);
        String actual = driver.getCapabilities().getBrowserName();
        assertTrue(format("actual browser name is %s", actual),
            actual.equals(expectedName));
    }

    @NotNull
    private static RemoteWebDriver setupDriverFromRule(BrowserWebDriverContainer rule) {
        RemoteWebDriver driver = rule.getWebDriver();
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
        return driver;
    }

    protected static void doSimpleExplore(BrowserWebDriverContainer rule) {
        RemoteWebDriver driver = setupDriverFromRule(rule);
        driver.get("http://en.wikipedia.org/wiki/Randomness");

        // Oh! The irony!
        assertTrue("Randomness' description has the word 'pattern'", driver.findElementByPartialLinkText("pattern").isDisplayed());
    }

}
