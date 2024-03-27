package org.testcontainers.qdrant;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers implementation for Qdrant.
 * <p>
 * Supported image: {@code qdrant/qdrant}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>HTTP: 6333</li>
 *     <li>Grpc: 6334</li>
 * </ul>
 */
public class QdrantContainer extends GenericContainer<QdrantContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("qdrant/qdrant");

    public QdrantContainer(String image) {
        this(DockerImageName.parse(image));
    }

    public QdrantContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        withExposedPorts(6333, 6334);
        waitingFor(Wait.forHttp("/readyz").forPort(6333));
    }

    public String getGrpcHostAddress() {
        return getHost() + ":" + getMappedPort(6334);
    }
}
