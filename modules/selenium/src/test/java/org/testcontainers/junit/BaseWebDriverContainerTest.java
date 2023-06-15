package org.testcontainers.junit;

import org.junit.ClassRule;
import org.openqa.selenium.By;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
public class BaseWebDriverContainerTest {

    @ClassRule
    public static Network NETWORK = Network.newNetwork();

    @ClassRule
    public static GenericContainer<?> HELLO_WORLD = new GenericContainer<>(
        DockerImageName.parse("testcontainers/helloworld:1.1.0")
    )
        .withNetwork(NETWORK)
        .withNetworkAliases("helloworld")
        .withExposedPorts(8080, 8081)
        .waitingFor(new HttpWaitStrategy());

    protected static void doSimpleExplore(BrowserWebDriverContainer<?> rule, Capabilities capabilities) {
        RemoteWebDriver driver = new RemoteWebDriver(rule.getSeleniumAddress(), capabilities);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(30));
        System.out.println("Selenium remote URL is: " + rule.getSeleniumAddress());
        System.out.println("VNC URL is: " + rule.getVncAddress());

        driver.get("http://helloworld:8080");
        WebElement title = driver.findElement(By.tagName("h1"));

        assertThat(title.getText().trim())
            .as("the index page contains the title 'Hello world'")
            .isEqualTo("Hello world");
        driver.quit();
    }

    protected void assertBrowserNameIs(
        BrowserWebDriverContainer<?> rule,
        String expectedName,
        Capabilities capabilities
    ) {
        RemoteWebDriver driver = new RemoteWebDriver(rule.getSeleniumAddress(), capabilities);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(30));
        String actual = driver.getCapabilities().getBrowserName();
        assertThat(actual).as(String.format("actual browser name is %s", actual)).isEqualTo(expectedName);
        driver.quit();
    }
}
