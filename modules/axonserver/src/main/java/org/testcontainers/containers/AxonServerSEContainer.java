package org.testcontainers.containers;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Constructs a single node AxonServer Standard Edition (SE) for testing.
 */
@Slf4j
public class AxonServerSEContainer extends GenericContainer<AxonServerSEContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("axoniq/axonserver");
    private static final int AXON_SERVER_HTTP_PORT = 8024;
    private static final int AXON_SERVER_GRPC_PORT = 8124;

    private static final String AXON_SERVER_ADDRESS_TEMPLATE = "%s:%s";

    public AxonServerSEContainer(@NonNull final String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public AxonServerSEContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);

        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        withExposedPorts(AXON_SERVER_HTTP_PORT, AXON_SERVER_GRPC_PORT);
        waitingFor(Wait.forLogMessage(".*Started AxonServer.*", 1));
    }

    public Integer getGrpcPort() {
        return this.getMappedPort(AXON_SERVER_GRPC_PORT);
    }

    public String getIPAddress() {
        return this.getContainerIpAddress();
    }

    public String getAxonServerAddress() {
        return String.format(AXON_SERVER_ADDRESS_TEMPLATE,
                             this.getContainerIpAddress(),
                             this.getMappedPort(AXON_SERVER_GRPC_PORT));
    }
}
