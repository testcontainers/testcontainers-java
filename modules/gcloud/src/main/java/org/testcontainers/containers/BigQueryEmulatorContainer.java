package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers implementation for BigQuery.
 */
public class BigQueryEmulatorContainer extends GenericContainer<BigQueryEmulatorContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("ghcr.io/goccy/bigquery-emulator");

    private static final int HTTP_PORT = 9050;

    private static final int GRPC_PORT = 9060;

    public BigQueryEmulatorContainer(String image) {
        this(DockerImageName.parse(image));
    }

    public BigQueryEmulatorContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        addExposedPorts(HTTP_PORT, GRPC_PORT);
        withCommand("--project", "test");
    }

    public String getEmulatorHttpEndpoint() {
        return getHost() + ":" + getMappedPort(HTTP_PORT);
    }
}
