package org.testcontainers.containers;

import com.sun.net.httpserver.HttpServer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.Testcontainers;

import java.io.OutputStream;
import java.net.InetSocketAddress;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

public class ExposedHostTest {

    private static HttpServer server;

    @BeforeClass
    public static void setUpClass() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/", exchange -> {
            byte[] content = "Hello World!".getBytes();
            exchange.sendResponseHeaders(200, content.length);
            try (OutputStream responseBody = exchange.getResponseBody()) {
                responseBody.write(content);
                responseBody.flush();
            }
        });

        server.start();
        Testcontainers.exposeHostPorts(server.getAddress().getPort());
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        server.stop(0);
    }

    @Test
    public void testExposedHost() throws Exception {
        try (GenericContainer container = new GenericContainer<>().withCommand("top")) {
            container.start();

            String response = container.execInContainer("wget", "-O", "-", "http://host.testcontainers.internal:" + server.getAddress().getPort()).getStdout();

            assertEquals("received response", "Hello World!", response);
        }
    }

    @Test
    public void testExposedHostWithNetwork() throws Exception {
        try (GenericContainer container = new GenericContainer<>().withNetwork(Network.newNetwork()).withCommand("top")) {
            container.start();

            String response = container.execInContainer("wget", "-O", "-", "http://host.testcontainers.internal:" + server.getAddress().getPort()).getStdout();

            assertEquals("received response", "Hello World!", response);
        }
    }
}
