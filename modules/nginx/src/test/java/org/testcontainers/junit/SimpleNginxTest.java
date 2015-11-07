package org.testcontainers.junit;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;
import org.testcontainers.containers.BrowserWebDriverContainer;
import org.testcontainers.containers.NginxContainer;

import java.io.*;
import java.net.URLConnection;

import static org.rnorth.visibleassertions.VisibleAssertions.*;


/**
 * @author richardnorth
 */
public class SimpleNginxTest {

    @Rule
    public NginxContainer nginx = new NginxContainer()
            .withCustomContent(System.getProperty("user.home") + "/.tmp-test-container");

    @Rule
    public BrowserWebDriverContainer chrome = new BrowserWebDriverContainer()
            .withDesiredCapabilities(DesiredCapabilities.chrome())
            .withLinkToContainer(nginx, "nginx");

    @BeforeClass
    public static void setupContent() throws FileNotFoundException {
        File contentFolder = new File(System.getProperty("user.home") + "/.tmp-test-container");
        contentFolder.mkdir();
        contentFolder.setReadable(true, false);
        contentFolder.setWritable(true, false);
        contentFolder.setExecutable(true, false);


        File indexFile = new File(contentFolder, "index.html");
        indexFile.setReadable(true, false);
        indexFile.setWritable(true, false);
        indexFile.setExecutable(true, false);

        PrintStream printStream = new PrintStream(new FileOutputStream(indexFile));
        printStream.println("<html><body>This worked</body></html>");
        printStream.close();
    }

    @Test
    public void testSimple() throws Exception {

        info("Base URL is " + nginx.getBaseUrl("http", 80));

        URLConnection urlConnection = nginx.getBaseUrl("http", 80).openConnection();
        String line = new BufferedReader(new InputStreamReader(urlConnection.getInputStream())).readLine();
        System.out.println(line);

        assertTrue("Using URLConnection, an HTTP GET from the nginx server returns the index.html from the custom content directory", line.contains("This worked"));
    }

    @Test
    public void testWebDriverToNginxContainerAccessViaContainerLink() throws Exception {

        info("Base URL is " + nginx.getBaseUrl("http", 80));

        RemoteWebDriver driver = chrome.getWebDriver();

        driver.get("http://nginx/");

        assertEquals("Using selenium, an HTTP GET from the nginx server returns the index.html from the custom content directory", "This worked", driver.findElement(By.tagName("body")).getText());
    }
}
