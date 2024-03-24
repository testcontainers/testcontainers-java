package org.testcontainers.qdrant;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

/**
 * Testcontainers implementation for Qdrant.
 *
 * <p>Supported image: {@code qdrant/qdrant}
 *
 * <p>Exposed ports:
 *
 * <ul>
 *   <li>HTTP: 6333
 *   <li>GRPC: 6334
 * </ul>
 */
public class QdrantContainer extends GenericContainer<QdrantContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("qdrant/qdrant");

    private final int QDRANT_REST_PORT = 6333;

    private final int QDRANT_GRPC_PORT = 6334;

    private final String CONFIG_FILE_PATH = "/qdrant/config/config.yaml";

    private final String API_KEY_ENV = "QDRANT__SERVICE__API_KEY";

    public QdrantContainer(String image) {
        this(DockerImageName.parse(image));
    }

    public QdrantContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        withExposedPorts(QDRANT_REST_PORT, QDRANT_GRPC_PORT);
        waitingFor(Wait.forHttp("/readyz").forPort(QDRANT_REST_PORT));
    }

    public QdrantContainer withApiKey(String apiKey) {
        return withEnv(API_KEY_ENV, apiKey);
    }

    public QdrantContainer withConfigFile(MountableFile configFile) {
        return withCopyFileToContainer(configFile, CONFIG_FILE_PATH);
    }

    public int getHttpPort() {
        return getMappedPort(QDRANT_REST_PORT);
    }

    public int getGrpcPort() {
        return getMappedPort(QDRANT_GRPC_PORT);
    }

    public String getRestHostAddress() {
        return getHost() + ":" + getHttpPort();
    }

    public String getGrpcHostAddress() {
        return getHost() + ":" + getGrpcPort();
    }
}
