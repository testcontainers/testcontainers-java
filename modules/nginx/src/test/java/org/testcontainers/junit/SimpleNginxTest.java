package org.testcontainers.junit;

import lombok.Cleanup;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.NginxContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

import java.io.*;
import java.net.URLConnection;

import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;
import static org.rnorth.visibleassertions.VisibleAssertions.info;


/**
 * @author richardnorth
 */
public class SimpleNginxTest {

    private static File contentFolder = new File(System.getProperty("user.home") + "/.tmp-test-container");

    @Rule
    public NginxContainer nginx = new NginxContainer<>()
        .withCustomContent(contentFolder.toString())
        .waitingFor(new HttpWaitStrategy());

    @SuppressWarnings({"Duplicates", "ResultOfMethodCallIgnored"})
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
    public void testSimple() throws Exception {

        info("Base URL is " + nginx.getBaseUrl("http", 80));

        URLConnection urlConnection = nginx.getBaseUrl("http", 80).openConnection();
        @Cleanup BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        String line = reader.readLine();
        System.out.println(line);

        assertTrue("Using URLConnection, an HTTP GET from the nginx server returns the index.html from the custom content directory", line.contains("This worked"));
    }
}
