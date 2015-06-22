package org.rnorth.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Random;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.rnorth.testcontainers.junit.BrowserWebDriverContainerRule.VncRecordingMode.RECORD_ALL;
import static org.rnorth.testcontainers.junit.BrowserWebDriverContainerRule.VncRecordingMode.RECORD_FAILING;

/**
 *
 */
public class SimpleWebDriverContainerTest {

    @Rule
    public BrowserWebDriverContainerRule chrome = new BrowserWebDriverContainerRule(DesiredCapabilities.chrome());

    @Rule
    public BrowserWebDriverContainerRule chromeThatRecordsAllTests = new BrowserWebDriverContainerRule(DesiredCapabilities.chrome(), RECORD_ALL, new File("./target/"));

    @Rule
    public BrowserWebDriverContainerRule chromeThatRecordsFailingTests = new BrowserWebDriverContainerRule(DesiredCapabilities.chrome(), RECORD_FAILING, new File("./target"));

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

    @Test
    public void recordingTestThatShouldBeRecordedButDeleted() {
        RemoteWebDriver driver = chromeThatRecordsFailingTests.newDriver();

        doSimpleExplore(driver);
    }

    @Test
    public void recordingTestThatShouldBeRecordedAndRetained() {
        RemoteWebDriver driver = chromeThatRecordsAllTests.newDriver();

        doSimpleExplore(driver);
    }

    protected void doSimpleExplore(RemoteWebDriver driver) {
        driver.get("http://en.wikipedia.org/wiki/Randomness");

        for (int i = 0; i < 5; i++) {
            Random random = new Random();
            Optional<WebElement> randomLink = driver.findElements(By.tagName("a")).stream()
                    .filter(element -> random.nextInt(10) == 0)
                    .filter(element -> element.isDisplayed() && element.isEnabled())
                    .findFirst();
            randomLink.ifPresent(WebElement::click);
        }
    }
}
