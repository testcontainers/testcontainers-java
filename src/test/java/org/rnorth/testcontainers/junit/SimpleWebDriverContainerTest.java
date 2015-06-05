package org.rnorth.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.IOException;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

/**
 *
 */
public class SimpleWebDriverContainerTest {

    @Rule
    public BrowserWebDriverContainerRule chrome = new BrowserWebDriverContainerRule(DesiredCapabilities.chrome());

    @Rule
    public BrowserWebDriverContainerRule firefox = new BrowserWebDriverContainerRule(DesiredCapabilities.firefox());

    @Test
    public void simpleTest() throws IOException {
        for (BrowserWebDriverContainerRule rule : asList(chrome, firefox)) {

            RemoteWebDriver driver = rule.newDriver();
            System.out.println("Selenium remote URL is: " + rule.getSeleniumURL(driver));
            System.out.println("VNC URL is: " + rule.getVncUrl(driver));

            //Runtime.getRuntime().exec("open " + rule.getVncUrl(driver)); // For debugging, on a Mac

            driver.get("http://www.google.com");
            driver.findElement(By.name("q")).sendKeys("testcontainers");
            driver.findElement(By.name("q")).submit();
            assertEquals("testcontainers", driver.findElement(By.name("q")).getAttribute("value"));
        }


    }
}
