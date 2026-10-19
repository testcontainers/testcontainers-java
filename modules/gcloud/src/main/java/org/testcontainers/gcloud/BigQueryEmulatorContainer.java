package org.testcontainers.gcloud;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers implementation for BigQuery.
 * <p>
 * Supported image: {@code ghcr.io/goccy/bigquery-emulator}
 * <p>
 */
public class BigQueryEmulatorContainer extends GenericContainer<BigQueryEmulatorContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("ghcr.io/goccy/bigquery-emulator");

    private static final int HTTP_PORT = 9050;

    private static final int GRPC_PORT = 9060;

    private static final String DEFAULT_PROJECT_ID = "test-project";

    private String projectId = DEFAULT_PROJECT_ID;

    public BigQueryEmulatorContainer(String image) {
        this(DockerImageName.parse(image));
    }

    public BigQueryEmulatorContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        addExposedPorts(HTTP_PORT, GRPC_PORT);
        withCommand("--project", DEFAULT_PROJECT_ID);
    }

    /**
     * Sets the project id the emulator is started with. Defaults to {@code test-project}.
     *
     * @param projectId the project id
     * @return this container
     */
    public BigQueryEmulatorContainer withProjectId(String projectId) {
        this.projectId = projectId;
        return withCommand("--project", projectId);
    }

    public String getEmulatorHttpEndpoint() {
        return String.format("http://%s:%d", getHost(), getMappedPort(HTTP_PORT));
    }

    public Integer getEmulatorGrpcPort() {
        return getMappedPort(GRPC_PORT);
    }

    public String getProjectId() {
        return this.projectId;
    }
}
