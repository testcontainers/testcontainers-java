package org.testcontainers.containers;

import java.util.HashMap;
import java.util.Map;

import org.mockserver.client.MockServerClient;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MockServerContainer extends GenericContainer<MockServerContainer> {

    public static final String VERSION = "5.5.1";

    public static final int PORT = 1080;

    private final Map<String,MockServerClient> clients = new HashMap<>();

    public MockServerContainer() {
        this(VERSION);
    }

    public MockServerContainer(String version) {
        super("jamesdbloom/mockserver:mockserver-" + version);
        withCommand("/opt/mockserver/run_mockserver.sh -logLevel INFO -serverPort " + PORT);
        addExposedPorts(PORT);
    }

    public String getEndpoint() {
        return String.format("http://%s:%d", getContainerIpAddress(), getMappedPort(PORT));
    }

    public Integer getServerPort() {
        return getMappedPort(PORT);
    }

    public MockServerClient getClient() {
        return getClient("");
    }

    public MockServerClient getClient(String path) {
        return clients.computeIfAbsent(path, (p) -> new MockServerClient(getContainerIpAddress(), getServerPort(), p));
    }

    @Override
    public void stop() {
        super.stop();
        clients.values().forEach(client -> client.close());
    }
}
