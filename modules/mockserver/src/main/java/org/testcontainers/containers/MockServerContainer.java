package org.testcontainers.containers;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.utility.DockerImageName;

@Slf4j
public class MockServerContainer extends GenericContainer<MockServerContainer> {

    public static final String VERSION = "5.5.4";

    public static final int PORT = 1080;

    /**
     * @deprecated use {@link MockServerContainer(DockerImageName)} instead
     */
    @Deprecated
    public MockServerContainer() {
        this(VERSION);
    }

    /**
     * @deprecated use {@link MockServerContainer(DockerImageName)} instead
     */
    @Deprecated
    public MockServerContainer(String version) {
        this(DockerImageName.parse("jamesdbloom/mockserver:mockserver-" + version));
    }

    public MockServerContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        withCommand("-logLevel INFO -serverPort " + PORT);
        addExposedPorts(PORT);
    }

    public String getEndpoint() {
        return String.format("http://%s:%d", getHost(), getMappedPort(PORT));
    }

    public Integer getServerPort() {
        return getMappedPort(PORT);
    }
}
