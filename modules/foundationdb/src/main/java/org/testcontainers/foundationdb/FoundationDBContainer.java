package org.testcontainers.foundationdb;

import lombok.val;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.net.URL;

/**
 * Represents an elasticsearch docker instance which exposes by default port 9200 and 9300 (transport.tcp.port)
 * The docker image is by default fetched from docker.elastic.co/elasticsearch/elasticsearch
 */
public class FoundationDBContainer extends GenericContainer<FoundationDBContainer> {

    /**
     * FoundationDB Default HTTP port
     */
    private static final int FOUNDATIONDB_DEFAULT_PORT = 4500;


    private static final String FDB_NETWORKING_MODE_KEY = "FDB_NETWORKING_MODE";
    /**
     * Elasticsearch Docker base image
     */
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("foundationdb/foundationdb");

    /**
     * Elasticsearch Default version
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
     * Create an Elasticsearch Container by passing the full docker image name
     * @param dockerImageName Full docker image name as a {@link String}, like: docker.elastic.co/elasticsearch/elasticsearch:7.9.2
     */
    public FoundationDBContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    /**
     * Create an Elasticsearch Container by passing the full docker image name
     * @param dockerImageName Full docker image name as a {@link DockerImageName}, like: DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch:7.9.2")
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
