package org.testcontainers.junit;

import static java.lang.String.format;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;
import org.junit.ClassRule;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

/**
 *
 */
public class BaseWebDriverContainerTest {

    @ClassRule
    public static Network NETWORK = Network.newNetwork();

    @ClassRule
    public static GenericContainer<?> HELLO_WORLD = new GenericContainer<>(DockerImageName.parse("testcontainers/helloworld:1.1.0"))
        .withNetwork(NETWORK)
        .withNetworkAliases("helloworld")
        .withExposedPorts(8080, 8081)
        .waitingFor(new HttpWaitStrategy());

    protected static void doSimpleExplore(BrowserWebDriverContainer<?> rule) {
        RemoteWebDriver driver = setupDriverFromRule(rule);
        System.out.println("Selenium remote URL is: " + rule.getSeleniumAddress());
        System.out.println("VNC URL is: " + rule.getVncAddress());

        driver.get("http://helloworld:8080");
        WebElement title = driver.findElement(By.tagName("h1"));

        assertEquals("the index page contains the title 'Hello world'",
            "Hello world",
            title.getText().trim()
        );
    }

    protected void assertBrowserNameIs(BrowserWebDriverContainer<?> rule, String expectedName) {
        RemoteWebDriver driver = setupDriverFromRule(rule);
        String actual = driver.getCapabilities().getBrowserName();
        assertTrue(format("actual browser name is %s", actual),
            actual.equals(expectedName));
    }

    @NotNull
    private static RemoteWebDriver setupDriverFromRule(BrowserWebDriverContainer<?> rule) {
        RemoteWebDriver driver = rule.getWebDriver();
        driver.manage().timeouts().implicitlyWait(30, TimeUnit.SECONDS);
        return driver;
    }
}
