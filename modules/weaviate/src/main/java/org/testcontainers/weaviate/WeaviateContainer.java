package org.testcontainers.weaviate;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers implementation of Weaviate.
 * <p>
 * Supported image: {@code semitechnologies/weaviate}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>HTTP: 8080</li>
 *     <li>gRPC: 50051</li>
 * </ul>
 */
public class WeaviateContainer extends GenericContainer<WeaviateContainer> {

    private static final String WEAVIATE_IMAGE = "semitechnologies/weaviate";

    public WeaviateContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public WeaviateContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DockerImageName.parse(WEAVIATE_IMAGE));
        withExposedPorts(8080, 50051);
        withEnv("AUTHENTICATION_ANONYMOUS_ACCESS_ENABLED", "true");
        withEnv("PERSISTENCE_DATA_PATH", "/var/lib/weaviate");
    }

    public String getHttpHostAddress() {
        return getHost() + ":" + getMappedPort(8080);
    }

    public String getGrpcHostAddress() {
        return getHost() + ":" + getMappedPort(50051);
    }
}
