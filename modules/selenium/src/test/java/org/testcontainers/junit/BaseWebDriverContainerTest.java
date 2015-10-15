package org.testcontainers.junit;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.containers.BrowserWebDriverContainer;

import java.util.List;
import java.util.Random;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

/**
 *
 */
public class BaseWebDriverContainerTest {

    protected void doSimpleWebdriverTest(BrowserWebDriverContainer rule) {
        RemoteWebDriver driver = rule.getWebDriver();
        System.out.println("Selenium remote URL is: " + rule.getSeleniumAddress());
        System.out.println("VNC URL is: " + rule.getVncAddress());

        //Runtime.getRuntime().exec("open " + rule.getVncUrl(driver)); // For debugging, on a Mac

        driver.get("http://www.google.com");
        driver.findElement(By.name("q")).sendKeys("testcontainers");
        driver.findElement(By.name("q")).submit();
        assertEquals("the word 'testcontainers' appears in the search box", "testcontainers", driver.findElement(By.name("q")).getAttribute("value"));
    }

    protected void doSimpleExplore(BrowserWebDriverContainer rule) {
        RemoteWebDriver driver = rule.getWebDriver();
        driver.get("http://en.wikipedia.org/wiki/Randomness");

        loop:
        for (int i = 0; i < 5; i++) {
            Random random = new Random();
            List<WebElement> webElements = driver.findElements(By.tagName("a"));
            for (WebElement webElement : webElements) {
                if (random.nextInt(10) == 0 && webElement.isDisplayed() && webElement.isEnabled()) {
                    webElement.click();
                    break loop;
                }
            }
        }
    }

}
