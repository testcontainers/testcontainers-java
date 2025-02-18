package org.testcontainers.pinecone;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers implementation for Pinecone.
 * <p>
 * Exposed port: 5080
 */
public class PineconeContainer extends GenericContainer<PineconeContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse(
        "ghcr.io/pinecone-io/pinecone-local"
    );

    private static final int PORT = 5080;

    public PineconeContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public PineconeContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        withEnv("PORT", String.valueOf(5080));
        withExposedPorts(5080);
    }

    public String getEndpoint() {
        return "http://" + getHost() + ":" + getMappedPort(PORT);
    }
}
