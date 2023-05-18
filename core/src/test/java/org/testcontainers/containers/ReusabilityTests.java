package org.testcontainers.containers;

import com.sun.net.httpserver.HttpServer;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.testcontainers.TestImages;
import org.testcontainers.Testcontainers;
import org.testcontainers.utility.MockTestcontainersConfigurationRule;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.io.OutputStream;
import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

public class ReusabilityTests {

    private static HttpServer server;

    @BeforeClass
    public static void setUpClass() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext(
            "/",
            exchange -> {
                byte[] content = "Hello World!".getBytes();
                exchange.sendResponseHeaders(200, content.length);
                try (OutputStream responseBody = exchange.getResponseBody()) {
                    responseBody.write(content);
                    responseBody.flush();
                }
            }
        );

        server.start();
    }

    @AfterClass
    public static void tearDownClass() {
        server.stop(0);
    }

    @After
    public void tearDown() {
        PortForwardingContainer.INSTANCE.reset();
    }

    @Rule
    public MockTestcontainersConfigurationRule configurationMock = new MockTestcontainersConfigurationRule();

    @Test
    public void testExposedHostPortIsReusable() {
        Mockito.doReturn(true).when(TestcontainersConfiguration.getInstance()).environmentSupportsReuse();
        Testcontainers.exposeHostPorts(server.getAddress().getPort());
        GenericContainer reusableContainer = new GenericContainer<>(TestImages.TINY_IMAGE)
            .withCommand("top")
            .withReuse(true);
        GenericContainer reusedContainer = new GenericContainer<>(TestImages.TINY_IMAGE)
            .withCommand("top")
            .withReuse(true);

        reusableContainer.start();
        reusableContainer.waitUntilContainerStarted();
        assertResponse(reusableContainer, server.getAddress().getPort());
        reusedContainer.start();
        reusedContainer.waitUntilContainerStarted();
        assertThat(reusedContainer.getContainerId()).isEqualTo(reusableContainer.getContainerId());
        assertResponse(reusedContainer, server.getAddress().getPort());
    }

    @SneakyThrows
    protected void assertResponse(GenericContainer<?> container, int port) {
        String response = container
            .execInContainer("wget", "-O", "-", "http://host.testcontainers.internal:" + port)
            .getStdout();

        assertThat(response).as("received response").isEqualTo("Hello World!");
    }
}
