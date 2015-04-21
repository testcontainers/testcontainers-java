package org.rnorth.testcontainers.junit;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.rnorth.testcontainers.junit.NginxContainerRule;

import java.io.*;
import java.net.URLConnection;

import static org.junit.Assert.assertTrue;

/**
 * @author richardnorth
 */
public class SimpleNginxTest {

    @Rule
    public NginxContainerRule nginx = new NginxContainerRule()
                                                    .withCustomConfig(System.getProperty("user.home") + "/.tmp-testpackage-container");

    @BeforeClass
    public static void setupContent() throws FileNotFoundException {
        File contentFolder = new File(System.getProperty("user.home") + "/.tmp-testpackage-container");
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

        assertTrue(line.contains("This worked"));
    }
}
