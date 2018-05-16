package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.mockserver.client.server.MockServerClient;

@Slf4j
public class MockServerContainer extends GenericContainer<MockServerContainer> {

    public static final String VERSION = "5.3.0";

    public static final int PORT = 80;

    @Getter
    private MockServerClient client;

    public MockServerContainer() {
        this(VERSION);
    }

    public MockServerContainer(String version) {
        super("jamesdbloom/mockserver:mockserver-" + version);
        withCommand("/opt/mockserver/run_mockserver.sh -logLevel INFO -serverPort " + PORT);
        addExposedPorts(PORT);
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        super.containerIsStarted(containerInfo);

        client = new MockServerClient(getContainerIpAddress(), getMappedPort(PORT));
    }

    public String getEndpoint() {
        return String.format("http://%s:%d", getContainerIpAddress(), getMappedPort(PORT));
    }
}
