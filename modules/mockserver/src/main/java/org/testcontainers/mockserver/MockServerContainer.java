package org.testcontainers.mockserver;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * @deprecated Use the MockServer-maintained module instead:
 * {@code org.mock-server:mockserver-testcontainers}
 * (class {@code org.mockserver.testcontainers.MockServerContainer}). It tracks current MockServer
 * releases, derives its image tag from the client library so the two stay in lockstep, and adds
 * configuration helpers (DNS, transparent proxy, HTTP/3, initialization JSON, log level, arbitrary
 * properties) plus direct {@code MockServerClient} wiring. See
 * https://www.mock-server.com/mock_server/mockserver_testcontainers.html
 */
@Slf4j
@Deprecated
public class MockServerContainer extends GenericContainer<MockServerContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("mockserver/mockserver");

    private static final String DEFAULT_TAG = "mockserver-7.0.0";

    @Deprecated
    public static final String VERSION = DEFAULT_TAG;

    public static final int PORT = 1080;

    /**
     * @deprecated use {@link #MockServerContainer(DockerImageName)} instead
     */
    @Deprecated
    public MockServerContainer(String version) {
        this(DEFAULT_IMAGE_NAME.withTag("mockserver-" + version));
    }

    public MockServerContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME, DockerImageName.parse("mockserver/mockserver"));

        waitingFor(Wait.forLogMessage(".*started on port: " + PORT + ".*", 1));

        withCommand("-serverPort " + PORT);
        addExposedPorts(PORT);
    }

    public String getEndpoint() {
        return String.format("http://%s:%d", getHost(), getMappedPort(PORT));
    }

    public String getSecureEndpoint() {
        return String.format("https://%s:%d", getHost(), getMappedPort(PORT));
    }

    public Integer getServerPort() {
        return getMappedPort(PORT);
    }
}
