package org.testcontainers.elasticsearch;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.Base58;
import org.testcontainers.utility.DockerImageName;

import java.net.InetSocketAddress;
import java.time.Duration;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

/**
 * Represents an elasticsearch docker instance which exposes by default port 9200 and 9300 (transport.tcp.port)
 * The docker image is by default fetched from docker.elastic.co/elasticsearch/elasticsearch
 */
public class ElasticsearchContainer extends GenericContainer<ElasticsearchContainer> {

    /**
     * Elasticsearch Default HTTP port
     */
    private static final int ELASTICSEARCH_DEFAULT_PORT = 9200;

    /**
     * Elasticsearch Default Transport port
     */
    private static final int ELASTICSEARCH_DEFAULT_TCP_PORT = 9300;

    /**
     * Elasticsearch Docker base image
     */
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("docker.elastic.co/elasticsearch/elasticsearch");

    /**
     * Elasticsearch Default version
     */
    protected static final String DEFAULT_TAG = "6.4.1";

    /**
     * @deprecated use {@link ElasticsearchContainer(DockerImageName)} instead
     */
    @Deprecated
    public ElasticsearchContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    /**
     * Create an Elasticsearch Container by passing the full docker image name
     * @param dockerImageName Full docker image name as a {@link String}, like: docker.elastic.co/elasticsearch/elasticsearch:6.4.1
     */
    public ElasticsearchContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    /**
     * Create an Elasticsearch Container by passing the full docker image name
     * @param dockerImageName Full docker image name as a {@link DockerImageName}, like: docker.elastic.co/elasticsearch/elasticsearch:6.4.1
     */
    public ElasticsearchContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);

        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        logger().info("Starting an elasticsearch container using [{}]", dockerImageName);
        withNetworkAliases("elasticsearch-" + Base58.randomString(6));
        withEnv("discovery.type", "single-node");
        addExposedPorts(ELASTICSEARCH_DEFAULT_PORT, ELASTICSEARCH_DEFAULT_TCP_PORT);
        setWaitStrategy(new HttpWaitStrategy()
            .forPort(ELASTICSEARCH_DEFAULT_PORT)
            .forStatusCodeMatching(response -> response == HTTP_OK || response == HTTP_UNAUTHORIZED)
            .withStartupTimeout(Duration.ofMinutes(2)));
    }

    public String getHttpHostAddress() {
        return getHost() + ":" + getMappedPort(ELASTICSEARCH_DEFAULT_PORT);
    }

    public InetSocketAddress getTcpHost() {
        return new InetSocketAddress(getHost(), getMappedPort(ELASTICSEARCH_DEFAULT_TCP_PORT));
    }
}
