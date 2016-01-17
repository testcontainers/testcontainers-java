package org.testcontainers.junit;

import org.jetbrains.annotations.NotNull;
import org.openqa.selenium.By;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.containers.BrowserWebDriverContainer;

import java.util.concurrent.TimeUnit;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

/**
 *
 */
public class BaseWebDriverContainerTest {

    protected void doSimpleWebdriverTest(BrowserWebDriverContainer rule) {
        RemoteWebDriver driver = setupDriverFromRule(rule);
        System.out.println("Selenium remote URL is: " + rule.getSeleniumAddress());
        System.out.println("VNC URL is: " + rule.getVncAddress());

        //Runtime.getRuntime().exec("open " + rule.getVncUrl(driver)); // For debugging, on a Mac

        driver.get("http://www.google.com");
        driver.findElement(By.name("q")).sendKeys("testcontainers");
        driver.findElement(By.name("q")).submit();
        assertEquals("the word 'testcontainers' appears in the search box", "testcontainers", driver.findElement(By.name("q")).getAttribute("value"));
    }

    @NotNull
    private RemoteWebDriver setupDriverFromRule(BrowserWebDriverContainer rule) {
        RemoteWebDriver driver = rule.getWebDriver();
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
        return driver;
    }

    protected void doSimpleExplore(BrowserWebDriverContainer rule) {
        RemoteWebDriver driver = setupDriverFromRule(rule);
        driver.get("http://en.wikipedia.org/wiki/Randomness");
    }

}
