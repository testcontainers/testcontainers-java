package org.testcontainers.containers;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MockServerContainer extends GenericContainer<MockServerContainer> {

    public static final String VERSION = "5.4.1";

    public static final int PORT = 80;

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
}
