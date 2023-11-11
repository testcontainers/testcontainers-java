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

    private static final String PROJECT_ID = "test-project";

    private int httpPort;
    
    private int grpcPort;

    public BigQueryEmulatorContainer(String image) {
        this(DockerImageName.parse(image));
    }

    public BigQueryEmulatorContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        httpPort = findFreePort();
        grpcPort = findFreePort();
        withCommand("--project="+ PROJECT_ID, "--port="+ httpPort, "--grpc-port="+ grpcPort);
        addFixedExposedPort(httpPort, httpPort);
        addFixedExposedPort(grpcPort, grpcPort);
    }

    public String getEmulatorHttpEndpoint() {
        return String.format("http://%s:%d", getHost(), getMappedPort(HTTP_PORT));
    }

    public String getEmulatorHttpHostAndPort() {
        return String.format("%s:%d", getHost(), httpPort);
    }

    public String getEmulatorGrpcHostAndPort() {
        return String.format("%s:%d", getHost(), grpcPort);
    }

    public String getProjectId() {
        return PROJECT_ID;
    }

    private static int findFreePort() {
      try (ServerSocket serverSocket = new ServerSocket(0)) {
        return serverSocket.getLocalPort();
      } catch (IOException ex) {
        throw new RuntimeException("could not find a free port", ex);
      }
    }
}
