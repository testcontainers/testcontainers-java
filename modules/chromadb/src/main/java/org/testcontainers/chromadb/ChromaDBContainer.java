package org.testcontainers.chromadb;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class ChromaDBContainer extends GenericContainer<ChromaDBContainer> {

    private static final DockerImageName DEFAULT_DOCKER_IMAGE = DockerImageName.parse("chromadb/chroma");

    private static final DockerImageName GHCR_DOCKER_IMAGE = DockerImageName.parse("ghcr.io/chroma-core/chroma");

    public ChromaDBContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public ChromaDBContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_DOCKER_IMAGE, GHCR_DOCKER_IMAGE);
        withExposedPorts(8000);
        waitingFor(Wait.forHttp("/api/v1/heartbeat"));
    }

    public String getEndpoint() {
        return "http://" + getHost() + ":" + getFirstMappedPort();
    }
}
