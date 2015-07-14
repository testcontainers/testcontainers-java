package org.rnorth.testcontainers.junit;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.io.*;
import java.net.URLConnection;

import static org.testpackage.VisibleAssertions.assertEquals;
import static org.testpackage.VisibleAssertions.assertTrue;

/**
 * @author richardnorth
 */
public class SimpleNginxTest {

    @Rule
    public NginxContainerRule nginx = new NginxContainerRule()
            .withCustomContent(System.getProperty("user.home") + "/.tmp-test-container")
            .withExposedPorts("80");

    @Rule
    public BrowserWebDriverContainerRule chrome = new BrowserWebDriverContainerRule(DesiredCapabilities.chrome())
            .withLinkToContainer(nginx, "nginx");

    @BeforeClass
    public static void setupContent() throws FileNotFoundException {
        File contentFolder = new File(System.getProperty("user.home") + "/.tmp-test-container");
        contentFolder.mkdir();
        File indexFile = new File(contentFolder, "index.html");

        PrintStream printStream = new PrintStream(new FileOutputStream(indexFile));
        printStream.println("<html><body>This worked</body></html>");
        printStream.close();
    }

    @Test
    public void testSimple() throws Exception {
        URLConnection urlConnection = nginx.getBaseUrl("http", 80).openConnection();
        String line = new BufferedReader(new InputStreamReader(urlConnection.getInputStream())).readLine();
        System.out.println(line);

        assertTrue("Using URLConnection, an HTTP GET from the nginx server returns the index.html from the custom content directory", line.contains("This worked"));
    }

    @Test
    public void testWebDriverToNginxContainerAccessViaContainerLink() throws Exception {

        RemoteWebDriver driver = chrome.newDriver();

        driver.get("http://nginx/");

        assertEquals("Using selenium, an HTTP GET from the nginx server returns the index.html from the custom content directory", "This worked", driver.findElement(By.tagName("body")).getText());
    }
}
