package org.testcontainers.containers;

import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@Slf4j
public class MockServerContainer extends GenericContainer<MockServerContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("jamesdbloom/mockserver");
    private static final String DEFAULT_TAG = "mockserver-5.5.4";

    @Deprecated
    public static final String VERSION = DEFAULT_TAG;

    public static final int PORT = 1080;

    /**
     * @deprecated use {@link MockServerContainer(DockerImageName)} instead
     */
    @Deprecated
    public MockServerContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    /**
     * @deprecated use {@link MockServerContainer(DockerImageName)} instead
     */
    @Deprecated
    public MockServerContainer(String version) {
        this(DEFAULT_IMAGE_NAME.withTag("mockserver-" + version));
    }

    public MockServerContainer(DockerImageName dockerImageName) {
        super(dockerImageName);

        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME, DockerImageName.parse("mockserver/mockserver"));

        waitingFor(Wait.forHttp("/mockserver/status").withMethod("PUT").forStatusCode(200));

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
