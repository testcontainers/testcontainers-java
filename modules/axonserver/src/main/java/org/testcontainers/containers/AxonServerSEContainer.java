package org.testcontainers.containers;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Constructs a single node AxonServer Standard Edition (SE) for testing.
 */
@Slf4j
public class AxonServerSEContainer<SELF extends AxonServerSEContainer<SELF>> extends GenericContainer<SELF> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("axoniq/axonserver");
    private static final int AXON_SERVER_HTTP_PORT = 8024;
    private static final int AXON_SERVER_GRPC_PORT = 8124;

    private static final String WAIT_FOR_LOG_MESSAGE = ".*Started AxonServer.*";

    private static final String AXONIQ_AXONSERVER_DEVMODE_ENABLED = "AXONIQ_AXONSERVER_DEVMODE_ENABLED";

    private static final String AXON_SERVER_ADDRESS_TEMPLATE = "%s:%s";

    private boolean devMode;

    public AxonServerSEContainer(@NonNull final String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public AxonServerSEContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);

        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        withExposedPorts(AXON_SERVER_HTTP_PORT, AXON_SERVER_GRPC_PORT);
        waitingFor(Wait.forLogMessage(WAIT_FOR_LOG_MESSAGE, 1));
    }

    @Override
    protected void configure() {
        withEnv(AXONIQ_AXONSERVER_DEVMODE_ENABLED, String.valueOf(devMode));
    }

    /**
     * Initialize AxonServer EE with a given license.
     */
    public SELF withDevMode(boolean devMode) {
        this.devMode = devMode;
        return self();
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
