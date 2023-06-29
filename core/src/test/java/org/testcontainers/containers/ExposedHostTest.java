package org.testcontainers.containers;

import com.google.common.collect.ImmutableMap;
import com.sun.net.httpserver.HttpServer;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.TestImages;
import org.testcontainers.Testcontainers;
import org.testcontainers.core.ContainerDef;

import java.io.OutputStream;
import java.net.InetSocketAddress;

import static org.assertj.core.api.Assertions.assertThat;

public class ExposedHostTest {

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

    @Test
    public void testExposedHostAfterContainerIsStarted() {
        try (
            GenericContainer<?> container = GenericContainer
                .with(ContainerDef.from(TestImages.TINY_IMAGE).withCommand("top"))
                .withAccessToHost(true)
        ) {
            container.start();
            Testcontainers.exposeHostPorts(server.getAddress().getPort());
            assertResponse(container, server.getAddress().getPort());
        }
    }

    @Test
    public void testExposedHost() {
        Testcontainers.exposeHostPorts(server.getAddress().getPort());
        assertResponse(
            GenericContainer.with(ContainerDef.from(TestImages.TINY_IMAGE).withCommand("top")),
            server.getAddress().getPort()
        );
    }

    @Test
    public void testExposedHostWithNetwork() {
        Testcontainers.exposeHostPorts(server.getAddress().getPort());
        try (Network network = Network.newNetwork()) {
            assertResponse(
                GenericContainer.with(ContainerDef.from(TestImages.TINY_IMAGE).withCommand("top")).withNetwork(network),
                server.getAddress().getPort()
            );
        }
    }

    @Test
    public void testExposedHostPortOnFixedInternalPorts() {
        Testcontainers.exposeHostPorts(ImmutableMap.of(server.getAddress().getPort(), 80));
        Testcontainers.exposeHostPorts(ImmutableMap.of(server.getAddress().getPort(), 81));

        assertResponse(GenericContainer.with(ContainerDef.from(TestImages.TINY_IMAGE).withCommand("top")), 80);
        assertResponse(GenericContainer.with(ContainerDef.from(TestImages.TINY_IMAGE).withCommand("top")), 81);
    }

    @SneakyThrows
    protected void assertResponse(GenericContainer<?> container, int port) {
        try {
            container.start();

            String response = container
                .execInContainer("wget", "-O", "-", "http://host.testcontainers.internal:" + port)
                .getStdout();

            assertThat(response).as("received response").isEqualTo("Hello World!");
        } finally {
            container.stop();
        }
    }
}
