package org.testcontainers.junit;

import lombok.Cleanup;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.NginxContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

import static org.hamcrest.CoreMatchers.containsString;
import static org.rnorth.visibleassertions.VisibleAssertions.*;

/**
 * @author richardnorth
 */
public class SimpleNginxTest {

    private static final DockerImageName NGINX_IMAGE = DockerImageName.parse("nginx:1.9.4");

    private static String tmpDirectory = System.getProperty("user.home") + "/.tmp-test-container";

    // creatingContainer {
    @Rule
    public NginxContainer<?> nginx = new NginxContainer<>(NGINX_IMAGE)
        .withCustomContent(tmpDirectory)
        .waitingFor(new HttpWaitStrategy());
    // }

    @SuppressWarnings({"Duplicates", "ResultOfMethodCallIgnored"})
    @BeforeClass
    public static void setupContent() throws Exception {
        // addCustomContent {
        // Create a temporary dir
        File contentFolder = new File(tmpDirectory);
        contentFolder.mkdir();
        contentFolder.deleteOnExit();

        // And "hello world" HTTP file
        File indexFile = new File(contentFolder, "index.html");
        indexFile.deleteOnExit();
        @Cleanup PrintStream printStream = new PrintStream(new FileOutputStream(indexFile));
        printStream.println("<html><body>Hello World!</body></html>");
        // }
    }

    @Test
    public void testSimple() throws Exception {
        // getFromNginxServer {
        URL baseUrl = nginx.getBaseUrl("http", 80);

        assertThat("An HTTP GET from the Nginx server returns the index.html from the custom content directory",
            responseFromNginx(baseUrl),
            containsString("Hello World!")
        );
        // }
    }

    private static String responseFromNginx(URL baseUrl) throws IOException {
        URLConnection urlConnection = baseUrl.openConnection();
        @Cleanup BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        return reader.readLine();
    }
}
