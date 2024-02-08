package org.testcontainers.containers;

import com.google.common.collect.ImmutableMap;
import com.sun.net.httpserver.HttpServer;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.TestImages;
import org.testcontainers.Testcontainers;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

public class ExposedHostTest {

    private static HttpServer server;
    private static Network network;

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
        network = createReusableNetwork(UUID.randomUUID());
    }

    @AfterClass
    public static void tearDownClass() {
        server.stop(0);
        DockerClientFactory.instance().client().removeNetworkCmd(network.getId()).exec();
    }

    @After
    public void tearDown() {
        PortForwardingContainer.INSTANCE.reset();
    }

    @Test
    public void testExposedHostAfterContainerIsStarted() {
        try (GenericContainer<?> container = new GenericContainer<>(tinyContainerDef()).withAccessToHost(true)) {
            container.start();
            Testcontainers.exposeHostPorts(server.getAddress().getPort());
            assertResponse(container, server.getAddress().getPort());
        }
    }

    @Test
    public void testExposedHost() {
        Testcontainers.exposeHostPorts(server.getAddress().getPort());
        assertResponse(new GenericContainer<>(tinyContainerDef()), server.getAddress().getPort());
    }

    @Test
    public void testExposedHostWithNetwork() {
        Testcontainers.exposeHostPorts(server.getAddress().getPort());
        try (Network network = Network.newNetwork()) {
            assertResponse(
                new GenericContainer<>(tinyContainerDef()).withNetwork(network),
                server.getAddress().getPort()
            );
        }
    }

    @Test
    public void testExposedHostPortOnFixedInternalPorts() {
        Testcontainers.exposeHostPorts(ImmutableMap.of(server.getAddress().getPort(), 80));
        Testcontainers.exposeHostPorts(ImmutableMap.of(server.getAddress().getPort(), 81));

        assertResponse(new GenericContainer<>(tinyContainerDef()), 80);
        assertResponse(new GenericContainer<>(tinyContainerDef()), 81);
    }

    @Test
    public void testExposedHostWithReusableContainerAndFixedNetworkName() throws IOException, InterruptedException {
        Testcontainers.exposeHostPorts(server.getAddress().getPort());

        try (
            GenericContainer<?> container = new GenericContainer<>(tinyContainerDef())
                .withReuse(true)
                .withNetwork(network)
        ) {
            container.start();

            assertHttpResponseFromHost(container, server.getAddress().getPort());

            PortForwardingContainer.INSTANCE.reset();
            Testcontainers.exposeHostPorts(server.getAddress().getPort());

            try (
                GenericContainer<?> reusedContainer = new GenericContainer<>(tinyContainerDef())
                    .withReuse(true)
                    .withNetwork(network)
            ) {
                reusedContainer.start();

                assertThat(reusedContainer.getContainerId()).isEqualTo(container.getContainerId());
                assertHttpResponseFromHost(reusedContainer, server.getAddress().getPort());
            }
        }
    }

    @Test
    public void testExposedHostOnFixedInternalPortsWithReusableContainerAndFixedNetworkName()
        throws IOException, InterruptedException {
        Testcontainers.exposeHostPorts(ImmutableMap.of(server.getAddress().getPort(), 1234));

        try (
            GenericContainer<?> container = new GenericContainer<>(tinyContainerDef())
                .withReuse(true)
                .withNetwork(network)
        ) {
            container.start();

            assertHttpResponseFromHost(container, 1234);

            PortForwardingContainer.INSTANCE.reset();
            Testcontainers.exposeHostPorts(ImmutableMap.of(server.getAddress().getPort(), 1234));

            try (
                GenericContainer<?> reusedContainer = new GenericContainer<>(tinyContainerDef())
                    .withReuse(true)
                    .withNetwork(network)
            ) {
                reusedContainer.start();

                assertThat(reusedContainer.getContainerId()).isEqualTo(container.getContainerId());
                assertHttpResponseFromHost(reusedContainer, 1234);
            }
        }
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

    private ContainerDef tinyContainerDef() {
        return new TinyContainerDef();
    }

    private static class TinyContainerDef extends ContainerDef {

        TinyContainerDef() {
            setImage(TestImages.TINY_IMAGE);
            setCommand("top");
        }
    }

    private void assertHttpResponseFromHost(GenericContainer<?> container, int port)
        throws IOException, InterruptedException {
        String httpResponseFromHost = container
            .execInContainer("wget", "-O", "-", "http://host.testcontainers.internal:" + port)
            .getStdout();
        assertThat(httpResponseFromHost).isEqualTo("Hello World!");
    }

    private static Network createReusableNetwork(UUID name) {
        String id = DockerClientFactory
            .instance()
            .client()
            .listNetworksCmd()
            .exec()
            .stream()
            .filter(network ->
                network.getName().equals(name.toString()) &&
                network.getLabels().equals(DockerClientFactory.DEFAULT_LABELS)
            )
            .map(com.github.dockerjava.api.model.Network::getId)
            .findFirst()
            .orElseGet(() ->
                DockerClientFactory
                    .instance()
                    .client()
                    .createNetworkCmd()
                    .withName(name.toString())
                    .withCheckDuplicate(true)
                    .withLabels(DockerClientFactory.DEFAULT_LABELS)
                    .exec()
                    .getId()
            );

        return new Network() {
            @Override
            public Statement apply(Statement base, Description description) {
                return base;
            }

            @Override
            public String getId() {
                return id;
            }

            @Override
            public void close() {
                // never close
            }
        };
    }
}
