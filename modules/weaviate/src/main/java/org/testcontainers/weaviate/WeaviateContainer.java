package org.testcontainers.weaviate;

import java.time.Duration;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers implementation of Weaviate.
 * <p>
 * Supported images: {@code cr.weaviate.io/semitechnologies/weaviate}, {@code semitechnologies/weaviate}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>HTTP: 8080</li>
 *     <li>gRPC: 50051</li>
 * </ul>
 */
public class WeaviateContainer extends GenericContainer<WeaviateContainer> {

    private static final DockerImageName DEFAULT_WEAVIATE_IMAGE = DockerImageName.parse(
        "cr.weaviate.io/semitechnologies/weaviate"
    );

    private static final DockerImageName DOCKER_HUB_WEAVIATE_IMAGE = DockerImageName.parse("semitechnologies/weaviate");

    public WeaviateContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public WeaviateContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_WEAVIATE_IMAGE, DOCKER_HUB_WEAVIATE_IMAGE);
        withExposedPorts(8080, 50051);
        withEnv("AUTHENTICATION_ANONYMOUS_ACCESS_ENABLED", "true");
        withEnv("PERSISTENCE_DATA_PATH", "/var/lib/weaviate");
        waitingFor(Wait.forHttp("/v1/.well-known/ready").forPort(8080).forStatusCode(200));
    }

    public String getHttpHostAddress() {
        return getHost() + ":" + getMappedPort(8080);
    }

    public String getGrpcHostAddress() {
        return getHost() + ":" + getMappedPort(50051);
    }
}
