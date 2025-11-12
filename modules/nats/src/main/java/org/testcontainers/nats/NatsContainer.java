package org.testcontainers.nats;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers implementation for NATS.
 * <p>
 * Supported image: {@code nats}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>4222 (Client connections)</li>
 *     <li>6222 (Cluster/Route connections)</li>
 *     <li>8222 (HTTP management/monitoring)</li>
 * </ul>
 */
public class NatsContainer extends GenericContainer<NatsContainer> {

    /**
     * Default port for NATS client connections.
     */
    public static final int DEFAULT_NATS_CLIENT_PORT = 4222;

    /**
     * Default port for NATS cluster/route connections.
     */
    public static final int DEFAULT_NATS_ROUTING_PORT = 6222;

    /**
     * Default port for NATS HTTP monitoring.
     */
    public static final int DEFAULT_NATS_HTTP_MONITORING_PORT = 8222;

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("nats");

    /**
     * Creates a NATS container using a specific docker image name.
     *
     * @param dockerImageName The docker image to use.
     */
    public NatsContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    /**
     * Creates a NATS container using a specific docker image.
     *
     * @param dockerImageName The docker image to use.
     */
    public NatsContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        withExposedPorts(
            DEFAULT_NATS_CLIENT_PORT, 
            DEFAULT_NATS_ROUTING_PORT, 
            DEFAULT_NATS_HTTP_MONITORING_PORT);
        waitingFor(Wait.forLogMessage(".*Server is ready.*", 1));
    }

    /**
     * Gets the port for client connections.
     *
     * @return The mapped port for client connections.
     */
    public Integer getClientPort() {
        return getMappedPort(DEFAULT_NATS_CLIENT_PORT);
    }

    /**
     * Gets the port for cluster/route connections.
     *
     * @return The mapped port for routing connections.
     */
    public Integer getRoutingPort() {
        return getMappedPort(DEFAULT_NATS_ROUTING_PORT);
    }

    /**
     * Gets the port for HTTP monitoring.
     *
     * @return The mapped port for HTTP monitoring.
     */
    public Integer getHttpMonitoringPort() {
        return getMappedPort(DEFAULT_NATS_HTTP_MONITORING_PORT);
    }

    /**
     * Gets the NATS connection URL.
     *
     * @return NATS URL for client connections in the format nats://host:port
     */
    public String getConnectionUrl() {
        return String.format("nats://%s:%d", getHost(), getClientPort());
    }

    /**
     * Gets the NATS monitoring endpoint URL.
     *
     * @return HTTP URL for monitoring endpoint in the format http://host:port
     */
    public String getHttpMonitoringUrl() {
        return String.format("http://%s:%d", getHost(), getHttpMonitoringPort());
    }

    /**
     * Enables JetStream for the NATS server.
     *
     * @return This container instance
     */
    public NatsContainer withJetStream() {
        withCommand("--jetstream");
        return this;
    }

    /**
     * Configures authentication with username and password.
     *
     * @param username The username for authentication
     * @param password The password for authentication
     * @return This container instance
     */
    public NatsContainer withAuth(String username, String password) {
        withCommand("--user", username, "--pass", password);
        return this;
    }

    /**
     * Enables debug logging for the NATS server.
     *
     * @return This container instance
     */
    public NatsContainer withDebug() {
        withCommand("-D");
        return this;
    }

    /**
     * Enables protocol tracing for the NATS server.
     *
     * @return This container instance
     */
    public NatsContainer withProtocolTracing() {
        withCommand("-V");
        return this;
    }
}
