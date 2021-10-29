package org.testcontainers.containers;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.Optional;

/**
 * Constructs a single node AxonServer Enterprise Edition (EE) for testing.
 */
@Slf4j
public class AxonServerEEContainer<SELF extends AxonServerEEContainer<SELF>> extends GenericContainer<SELF> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("axoniq/axonserver-enterprise");
    private static final int AXON_SERVER_HTTP_PORT = 8024;
    private static final int AXON_SERVER_GRPC_PORT = 8124;

    private static final String WAIT_FOR_LOG_MESSAGE = ".*Started AxonServer.*";

    private static final String LICENCE_DEFAULT_LOCATION = "/axonserver/config/axoniq.license";
    private static final String AUTO_CLUSTER_DEFAULT_LOCATION = "/axonserver/config/axonserver.properties";
    private static final String CLUSTER_TEMPLATE_DEFAULT_LOCATION = "/axonserver/cluster-template.yml";

    private static final String AXONIQ_LICENSE = "AXONIQ_LICENSE";
    private static final String AXONIQ_AXONSERVER_NAME = "AXONIQ_AXONSERVER_NAME";
    private static final String AXONIQ_AXONSERVER_INTERNAL_HOSTNAME = "AXONIQ_AXONSERVER_INTERNAL_HOSTNAME";
    private static final String AXONIQ_AXONSERVER_HOSTNAME = "AXONIQ_AXONSERVER_HOSTNAME";

    private static final String AXON_SERVER_ADDRESS_TEMPLATE = "%s:%s";

    private String licensePath;
    private String autoClusterPath;
    private String clusterTemplatePath;
    private String axonServerName;
    private String axonServerInternalHostname;
    private String axonServerHostname;

    public AxonServerEEContainer(@NonNull final String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public AxonServerEEContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);

        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        withExposedPorts(AXON_SERVER_HTTP_PORT, AXON_SERVER_GRPC_PORT);
        waitingFor(Wait.forLogMessage(WAIT_FOR_LOG_MESSAGE, 1));
        withEnv(AXONIQ_LICENSE, LICENCE_DEFAULT_LOCATION);
    }

    @Override
    protected void configure() {
        optionallyMapResourceParameterAsVolume(LICENCE_DEFAULT_LOCATION, licensePath);
        optionallyMapResourceParameterAsVolume(AUTO_CLUSTER_DEFAULT_LOCATION, autoClusterPath);
        optionallyMapResourceParameterAsVolume(CLUSTER_TEMPLATE_DEFAULT_LOCATION, clusterTemplatePath);
        withOptionalEnv(AXONIQ_AXONSERVER_NAME, axonServerName);
        withOptionalEnv(AXONIQ_AXONSERVER_HOSTNAME, axonServerHostname);
        withOptionalEnv(AXONIQ_AXONSERVER_INTERNAL_HOSTNAME, axonServerInternalHostname);
    }

    /**
     * Map (effectively replace) directory in Docker with the content of resourceLocation if resource location is not
     * null
     * <p>
     * Protected to allow for changing implementation by extending the class
     *
     * @param pathNameInContainer path in docker
     * @param resourceLocation    relative classpath to resource
     */
    protected void optionallyMapResourceParameterAsVolume(String pathNameInContainer, String resourceLocation) {
        Optional.ofNullable(resourceLocation)
                .map(MountableFile::forClasspathResource)
                .ifPresent(mountableFile -> withCopyFileToContainer(mountableFile, pathNameInContainer));
    }

    /**
     * Set an environment value if the value is present.
     * <p>
     * Protected to allow for changing implementation by extending the class
     *
     * @param key   environment key value, usually a constant
     * @param value environment value to be set
     */
    protected void withOptionalEnv(String key, String value) {
        Optional.ofNullable(value)
                .ifPresent(v -> withEnv(key, value));
    }

    /**
     * Initialize AxonServer EE with a given license.
     */
    public SELF withLicense(String licensePath) {
        this.licensePath = licensePath;
        return self();
    }

    /**
     * Initialize AxonServer EE with a given auto cluster configuration file.
     */
    public SELF withAutoCluster(String autoClusterPath) {
        this.autoClusterPath = autoClusterPath;
        return self();
    }

    /**
     * Initialize AxonServer EE with a given cluster template configuration file.
     */
    public SELF withClusterTemplate(String clusterTemplatePath) {
        this.clusterTemplatePath = clusterTemplatePath;
        return self();
    }

    /**
     * Initialize AxonServer EE with a given Axon Server Name.
     */
    public SELF withAxonServerName(String axonServerName) {
        this.axonServerName = axonServerName;
        return self();
    }

    /**
     * Initialize AxonServer EE with a given Axon Server Internal Hostname.
     */
    public SELF withAxonServerInternalHostname(String axonServerInternalHostname) {
        this.axonServerInternalHostname = axonServerInternalHostname;
        return self();
    }

    /**
     * Initialize AxonServer EE with a given Axon Server Hostname.
     */
    public SELF withAxonServerHostname(String axonServerHostname) {
        this.axonServerHostname = axonServerHostname;
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
