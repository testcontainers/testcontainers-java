package org.testcontainers.junit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Test of {@link FixedHostPortGenericContainer}. Note that this is not an example of typical use (usually, a container
 * should be a field on the test class annotated with @Rule or @TestRule). Instead, here, the lifecycle of the container
 * is managed completely within the test method to allow a free port to be found and assigned before the container
 * is started.
 */
public class FixedHostPortContainerTest {

    /**
     * Docker image for simple open-source HTTP echo server https://github.com/hashicorp/http-echo
     */
    private static final String HTTP_ECHO_IMAGE = "hashicorp/http-echo";

    /**
     * Default echo server port
     */
    private static final int ECHO_PORT = 5678;

    /**
     * test echo response
     */
    private static final String TEST_ECHO = "test-echo";

    /**
     * Waiting strategy for echo server start
     */
    private final WaitStrategy echoServerStartWaitStrategy = new LogMessageWaitStrategy()
        .withRegEx(".+Server is listening.+");

    @Test
    public void testFixedHostPortMapping() throws IOException {
        // first find a free port on the docker host that will work for testing
        GenericContainer echoServer = new GenericContainer(HTTP_ECHO_IMAGE)
            .withCommand(String.format("-text=%s", TEST_ECHO))
            .withExposedPorts(ECHO_PORT)
            .waitingFor(this.echoServerStartWaitStrategy);

        echoServer.start();
        final Integer unusedHostPort = echoServer.getMappedPort(ECHO_PORT);

        String content = this.getHttpResponse(unusedHostPort);

        assertThat("Returned echo does not match expected", content, equalTo(TEST_ECHO));

        echoServer.stop();

        // now starting echo server container mapped to known-as-free host port
        echoServer = new FixedHostPortGenericContainer(HTTP_ECHO_IMAGE)
            .withFixedExposedPort(unusedHostPort, ECHO_PORT)
            .withCommand(String.format("-text=%s", TEST_ECHO))
            .waitingFor(this.echoServerStartWaitStrategy);

        echoServer.start();

        assertThat("Port mapping does not seem to match given fixed port",
            echoServer.getMappedPort(ECHO_PORT), equalTo(unusedHostPort));

        content = this.getHttpResponse(unusedHostPort);

        assertThat("Returned echo from fixed port does not match expected", content, equalTo(TEST_ECHO));

        echoServer.stop();
    }

    /**
     * Simple pure-java web request/response functionality from localhost
     *
     * @param port to send request to
     * @return response content
     * @throws IOException if any
     */
    private String getHttpResponse(Integer port) throws IOException {
        final URL url = new URL(String.format("http://localhost:%d", port));
        final HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        final StringBuilder response;
        try (final BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
            String inputLine;
            response = new StringBuilder();
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        }
        con.disconnect();
        return response.toString();
    }
}
