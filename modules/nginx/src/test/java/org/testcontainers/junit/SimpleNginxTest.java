package org.testcontainers.junit;

import lombok.Cleanup;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.NginxContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.net.URLConnection;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleNginxTest {

    private static final DockerImageName NGINX_IMAGE = DockerImageName.parse("nginx:1.27.0-alpine3.19-slim");

    private static String tmpDirectory = System.getProperty("user.home") + "/.tmp-test-container";

    // creatingContainer {
    @Rule
    public NginxContainer nginx = new NginxContainer(NGINX_IMAGE)
        .withCopyFileToContainer(MountableFile.forHostPath(tmpDirectory), "/usr/share/nginx/html")
        .waitingFor(new HttpWaitStrategy());

    // }

    @SuppressWarnings({ "Duplicates", "ResultOfMethodCallIgnored" })
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
        @Cleanup
        PrintStream printStream = new PrintStream(new FileOutputStream(indexFile));
        printStream.println("<html><body>Hello World!</body></html>");
        // }
    }

    @Test
    public void testSimple() throws Exception {
        // getFromNginxServer {
        URL baseUrl = nginx.getBaseUrl("http", 80);

        assertThat(responseFromNginx(baseUrl))
            .as("An HTTP GET from the Nginx server returns the index.html from the custom content directory")
            .contains("Hello World!");
        // }
        assertHasCorrectExposedAndLivenessCheckPorts(nginx);
    }

    private void assertHasCorrectExposedAndLivenessCheckPorts(NginxContainer nginxContainer) {
        assertThat(nginxContainer.getExposedPorts()).containsExactly(80);
        assertThat(nginxContainer.getLivenessCheckPortNumbers()).containsExactly(nginxContainer.getMappedPort(80));
    }

    private static String responseFromNginx(URL baseUrl) throws IOException {
        URLConnection urlConnection = baseUrl.openConnection();
        @Cleanup
        BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
        return reader.readLine();
    }
}
