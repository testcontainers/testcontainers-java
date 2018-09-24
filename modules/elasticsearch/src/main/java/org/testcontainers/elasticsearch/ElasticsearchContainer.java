package org.testcontainers.elasticsearch;

import org.apache.http.HttpHost;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.Base58;

import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;

/**
 * Represents an elasticsearch docker instance which exposes by default port 9200 and 9300 (transport.tcp.port)
 * The docker image is by default fetch from docker.elastic.co/elasticsearch/elasticsearch
 * @author dadoonet
 */
public class ElasticsearchContainer extends GenericContainer {

    /**
     * Elasticsearch Default HTTP port
     */
    private static final int ELASTICSEARCH_DEFAULT_PORT = 9200;

    /**
     * Elasticsearch Default Transport port
     */
    private static final int ELASTICSEARCH_DEFAULT_TCP_PORT = 9300;

    /**
     * Elasticsearch Docker base URL
     */
    private static final String ELASTICSEARCH_DEFAULT_BASE_URL = "docker.elastic.co/elasticsearch/elasticsearch";

    /**
     * Elasticsearch Default version
     */
    private static final String ELASTICSEARCH_DEFAULT_VERSION = "6.4.1";

    public ElasticsearchContainer() {
        this(ELASTICSEARCH_DEFAULT_BASE_URL + ":" + ELASTICSEARCH_DEFAULT_VERSION);
    }

    /**
     * Create an Elasticsearch Container by passing the full docker image name
     * @param dockerImageName Full docker image name, like: docker.elastic.co/elasticsearch/elasticsearch:6.4.1
     */
    public ElasticsearchContainer(String dockerImageName) {
        super(dockerImageName);
        logger().info("Starting an elasticsearch container using [{}]", dockerImageName);
        withNetwork(Network.SHARED);
        withNetworkAliases("elasticsearch-" + Base58.randomString(6));
        addExposedPorts(ELASTICSEARCH_DEFAULT_PORT, ELASTICSEARCH_DEFAULT_TCP_PORT);
        setWaitStrategy(new HttpWaitStrategy()
            .forPort(ELASTICSEARCH_DEFAULT_PORT)
            .forStatusCodeMatching(response -> response == HTTP_OK || response == HTTP_UNAUTHORIZED));
    }

    public HttpHost getHost() {
        return new HttpHost(getContainerIpAddress(), getMappedPort(ELASTICSEARCH_DEFAULT_PORT));
    }
}
