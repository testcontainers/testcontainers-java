package org.testcontainers.junit;

import lombok.Cleanup;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.NginxContainer;

import java.io.*;

import static org.rnorth.visibleassertions.VisibleAssertions.*;


/**
 * @author richardnorth
 */
public class LinkedContainerTest {

    private static File contentFolder = new File(System.getProperty("user.home") + "/.tmp-test-container");

    @Rule
    public Network network = Network.newNetwork();

    @Rule
    public NginxContainer nginx = new NginxContainer<>()
            .withNetwork(network)
            .withNetworkAliases("nginx")
            .withCustomContent(contentFolder.toString());

    @Rule
    public BrowserWebDriverContainer chrome = new BrowserWebDriverContainer<>()
            .withNetwork(network)
            .withDesiredCapabilities(DesiredCapabilities.chrome());

    @BeforeClass
    public static void setupContent() throws FileNotFoundException {
        contentFolder.mkdir();
        contentFolder.setReadable(true, false);
        contentFolder.setWritable(true, false);
        contentFolder.setExecutable(true, false);


        File indexFile = new File(contentFolder, "index.html");
        indexFile.setReadable(true, false);
        indexFile.setWritable(true, false);
        indexFile.setExecutable(true, false);

        @Cleanup PrintStream printStream = new PrintStream(new FileOutputStream(indexFile));
        printStream.println("<html><body>This worked</body></html>");
    }

    @Test
    public void testWebDriverToNginxContainerAccessViaContainerLink() throws Exception {
        RemoteWebDriver driver = chrome.getWebDriver();

        driver.get("http://nginx/");

        assertEquals("Using selenium, an HTTP GET from the nginx server returns the index.html from the custom content directory", "This worked", driver.findElement(By.tagName("body")).getText());
    }
}
