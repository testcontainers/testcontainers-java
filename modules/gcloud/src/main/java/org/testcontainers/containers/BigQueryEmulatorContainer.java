package org.testcontainers.containers;

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

    private static final String PROJECT_ID = "test-project";

    public BigQueryEmulatorContainer(String image) {
        this(DockerImageName.parse(image));
    }

    public BigQueryEmulatorContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        addExposedPorts(HTTP_PORT, GRPC_PORT);
        withCommand("--project", PROJECT_ID);
    }

    public String getEmulatorHttpEndpoint() {
        return String.format("http://%s:%d", getHost(), getMappedPort(HTTP_PORT));
    }

    public String getProjectId() {
        return PROJECT_ID;
    }
}
