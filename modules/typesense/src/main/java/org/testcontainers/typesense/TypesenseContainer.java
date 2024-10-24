package org.testcontainers.typesense;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers implementation for Typesense.
 * <p>
 * Supported image: {@code typesense/typesense}
 * <p>
 * Exposed ports: 8108
 */
public class TypesenseContainer extends GenericContainer<TypesenseContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("typesense/typesense");

    private static final int PORT = 8108;

    private static final String DEFAULT_API_KEY = "testcontainers";

    private String apiKey = DEFAULT_API_KEY;

    public TypesenseContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public TypesenseContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        withExposedPorts(PORT);
        withEnv("TYPESENSE_DATA_DIR", "/tmp");
        waitingFor(
            Wait
                .forHttp("/health")
                .forStatusCode(200)
                .forResponsePredicate(response -> response.contains("\"ok\":true"))
        );
    }

    @Override
    protected void configure() {
        withEnv("TYPESENSE_API_KEY", this.apiKey);
    }

    public TypesenseContainer withApiKey(String apiKey) {
        this.apiKey = apiKey;
        return this;
    }

    public String getHttpPort() {
        return String.valueOf(getMappedPort(PORT));
    }

    public String getApiKey() {
        return this.apiKey;
    }
}
