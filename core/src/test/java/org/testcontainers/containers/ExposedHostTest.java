package org.testcontainers.containers;

import com.sun.net.httpserver.HttpServer;
import lombok.SneakyThrows;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.Testcontainers;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

public class ExposedHostTest {

    private static HttpServer server;
    private static HttpServer serverPortMapping;    

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
        
        serverPortMapping = HttpServer.create(new InetSocketAddress(0), 0);
        serverPortMapping.createContext("/", exchange -> {
            byte[] content = "Hello World!".getBytes();
            exchange.sendResponseHeaders(200, content.length);
            try (OutputStream responseBody = exchange.getResponseBody()) {
                responseBody.write(content);
                responseBody.flush();
            }
        });

        serverPortMapping.start();
        Map<Integer, Integer> portMapping = new HashMap<>();
        portMapping.put(serverPortMapping.getAddress().getPort(), 80);
        Testcontainers.exposeHostPorts(portMapping);        
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        server.stop(0);
        serverPortMapping.stop(0);        
    }

    @Test
    public void testExposedHost() throws Exception {
        assertResponse(new GenericContainer().withCommand("top"), server.getAddress().getPort());
    }

    @Test
    public void testExposedHostWithNetwork() throws Exception {
        try (Network network = Network.newNetwork()) {
            assertResponse(new GenericContainer().withNetwork(network).withCommand("top"), server.getAddress().getPort());
        }
    }
    
    @Test
    public void testExposedHostPortMapping() throws Exception {
        assertResponse(new GenericContainer().withCommand("top"), 80);
    }

    @Test
    public void testExposedHostWithNetworkPortMapping() throws Exception {
        try (Network network = Network.newNetwork()) {
            assertResponse(new GenericContainer().withNetwork(network).withCommand("top"), 80);
        }
    }    

    @SneakyThrows
    protected void assertResponse(GenericContainer container, int port) {
        try {
            container.start();

            String response = container.execInContainer("wget", "-O", "-", "http://host.testcontainers.internal:" + port).getStdout();

            assertEquals("received response", "Hello World!", response);
        } finally {
            container.stop();
        }
    }
}
