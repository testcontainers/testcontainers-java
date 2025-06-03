package org.testcontainers.junit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.testcontainers.TestImages;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.GenericContainer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test of {@link FixedHostPortGenericContainer}. Note that this is not an example of typical use (usually, a container
 * should be a field on the test class annotated with @Rule or @TestRule). Instead, here, the lifecycle of the container
 * is managed completely within the test method to allow a free port to be found and assigned before the container
 * is started.
 */
public class FixedHostPortContainerTest {

    private static final String TEST_IMAGE = "alpine:3.17";

    /**
     * Default http server port (just something different from default)
     */
    private static final int TEST_PORT = 5678;

    /**
     * test response
     */
    private static final String TEST_RESPONSE = "test-response";

    /**
     * *nix pipe to fire test response on test port
     */
    private static final String HTTP_ECHO_CMD = String.format(
        "while true; do echo \"%s\" | nc -l -p %d; done",
        TEST_RESPONSE,
        TEST_PORT
    );

    @Test
    public void testFixedHostPortMapping() throws IOException {
        // first find a free port on the docker host that will work for testing
        final Integer unusedHostPort;
        try (
            final GenericContainer echoServer = new GenericContainer(TestImages.TINY_IMAGE)
                .withExposedPorts(TEST_PORT)
                .withCommand("/bin/sh", "-c", HTTP_ECHO_CMD)
        ) {
            echoServer.start();
            unusedHostPort = echoServer.getMappedPort(TEST_PORT);
        }

        // now starting echo server container mapped to known-as-free host port
        try (
            final GenericContainer echoServer = new FixedHostPortGenericContainer(TEST_IMAGE)
                // using workaround for port bind+expose
                .withFixedExposedPort(unusedHostPort, TEST_PORT)
                .withExposedPorts(TEST_PORT)
                .withCommand("/bin/sh", "-c", HTTP_ECHO_CMD)
        ) {
            echoServer.start();

            assertThat(echoServer.getMappedPort(TEST_PORT))
                .as("Port mapping does not seem to match given fixed port")
                .isEqualTo(unusedHostPort);

            final String content = readResponse(echoServer, unusedHostPort);
            assertThat(content).as("Returned echo from fixed port does not match expected").isEqualTo(TEST_RESPONSE);
        }
    }

    /**
     * Simple socket content reader from given container:port
     *
     * @param container to query
     * @param port      to send request to
     * @return socket reader content
     * @throws IOException if any
     */
    private String readResponse(GenericContainer container, Integer port) throws IOException {
        try (
            Socket socket = Awaitility
                .await()
                .pollDelay(Duration.ofSeconds(1))
                .until(() -> new Socket(container.getHost(), port), Socket::isConnected);
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()))
        ) {
            return reader.readLine();
        }
    }
}
