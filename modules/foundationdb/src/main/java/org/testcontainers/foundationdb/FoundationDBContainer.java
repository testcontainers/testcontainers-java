package org.testcontainers.foundationdb;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Represents an foundationdb docker instance which exposes by default port 4500
 */
public class FoundationDBContainer extends GenericContainer<FoundationDBContainer> {

    /**
     * FoundationDB Default HTTP port
     */
    private static final int FOUNDATIONDB_DEFAULT_PORT = 4500;


    private static final String FDB_NETWORKING_MODE_KEY = "FDB_NETWORKING_MODE";
    /**
     * FoundationDB Docker base image
     */
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("foundationdb/foundationdb");

    /**
     * FoundationDB Default version
     */
    @Deprecated
    protected static final String DEFAULT_TAG = "6.2.28";
    private boolean isOss = false;

    /**
     * @deprecated use {@link FoundationDBContainer (DockerImageName)} instead
     */
    @Deprecated
    public FoundationDBContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    /**
     * Create an FoundationDB Container by passing the full docker image name
     * @param dockerImageName Full docker image name as a {@link DockerImageName}, like: DockerImageName.parse("foundationdb/foundationdb:6.2.28")
     */
    public FoundationDBContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    /**
     * Create an FoundationDB Container by passing the full docker image name
     * @param dockerImageName Full docker image name as a {@link DockerImageName}, like: DockerImageName.parse("foundationdb/foundationdb:6.2.28")
     */
    public FoundationDBContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);

        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        logger().info("Starting an foundationdb container using [{}]", dockerImageName);
        withEnv(FDB_NETWORKING_MODE_KEY, "host")
            .withFileSystemBind("./etc", "/etc/foundationdb");
        addExposedPorts(FOUNDATIONDB_DEFAULT_PORT);
        waitingFor(
            Wait.forLogMessage(".*FDBD joined cluster.*\\n", 1)
        );
    }

    public String getHttpHostAddress() {
        return getHost() + ":" + getMappedPort(FOUNDATIONDB_DEFAULT_PORT);
    }

}
