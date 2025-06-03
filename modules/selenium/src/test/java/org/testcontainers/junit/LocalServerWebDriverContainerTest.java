package org.testcontainers.junit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.bio.SocketConnector;
import org.mortbay.jetty.handler.ResourceHandler;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.Testcontainers;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.junit.jupiter.Container;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test that a browser running in a container can access a web server hosted on the host machine (i.e. the one running
 * the tests)
 */
@org.testcontainers.junit.jupiter.Testcontainers
public class LocalServerWebDriverContainerTest {

    @Container
    public BrowserWebDriverContainer<?> chrome = new BrowserWebDriverContainer<>()
        .withAccessToHost(true)
        .withCapabilities(new ChromeOptions());

    private int localPort;

    @BeforeEach
    public void setupLocalServer() throws Exception {
        // Set up a local Jetty HTTP server
        Server server = new Server();
        server.addConnector(new SocketConnector());
        ResourceHandler resourceHandler = new ResourceHandler();
        resourceHandler.setResourceBase("src/test/resources/server");
        server.addHandler(resourceHandler);
        server.start();

        // The server will have a random port assigned, so capture that
        localPort = server.getConnectors()[0].getLocalPort();
    }

    @Test
    public void testConnection() {
        // getWebDriver {
        RemoteWebDriver driver = new RemoteWebDriver(chrome.getSeleniumAddress(), new ChromeOptions());
        // }

        // Construct a URL that the browser container can access
        // getPage {
        Testcontainers.exposeHostPorts(localPort);
        driver.get("http://host.testcontainers.internal:" + localPort);
        // }

        String headingText = driver.findElement(By.cssSelector("h1")).getText().trim();

        assertThat(headingText)
            .as("The hardcoded success message was found on a page fetched from a local server")
            .isEqualTo("It worked");
    }
}
