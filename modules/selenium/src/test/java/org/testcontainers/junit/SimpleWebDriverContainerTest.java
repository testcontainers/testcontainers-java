package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Random;

import static java.util.Arrays.asList;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.testcontainers.junit.BrowserWebDriverContainerRule.VncRecordingMode.RECORD_ALL;
import static org.testcontainers.junit.BrowserWebDriverContainerRule.VncRecordingMode.RECORD_FAILING;

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
            assertEquals("the word 'testcontainers' appears in the search box", "testcontainers", driver.findElement(By.name("q")).getAttribute("value"));
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
