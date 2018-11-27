package org.testcontainers.containers;

import static java.net.HttpURLConnection.*;
import static java.util.stream.Collectors.*;

import java.time.Duration;
import java.util.Set;
import java.util.stream.Stream;

import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategy;

/**
 * Testcontainer for Neo4j.
 *
 * @param <S> "SELF" to be used in the <code>withXXX</code> methods.
 * @author Michael J. Simons
 */
public final class Neo4jContainer<S extends Neo4jContainer<S>> extends GenericContainer<S> {

    /**
     * The image defaults to the official Neo4j image: <a href="https://hub.docker.com/_/neo4j/">Neo4j</a>.
     */
    private static final String DEFAULT_IMAGE_NAME = "neo4j";

    /**
     * The default tag (version) to use.
     */
    private static final String DEFAULT_TAG = "3.4.10";

    private static final String DOCKER_IMAGE_NAME = DEFAULT_IMAGE_NAME + ":" + DEFAULT_TAG;

    /**
     * Default port for the binary Bolt protocol.
     */
    private static final int DEFAULT_BOLT_PORT = 7687;

    /**
     * The port of the transactional HTTPS endpoint: <a href="https://neo4j.com/docs/rest-docs/current/">Neo4j REST API</a>.
     */
    private static final int DEFAULT_HTTPS_PORT = 7473;

    /**
     * The port of the transactional HTTP endpoint: <a href="https://neo4j.com/docs/rest-docs/current/">Neo4j REST API</a>.
     */
    private static final int DEFAULT_HTTP_PORT = 7474;

    /**
     * The official image requires a change of password by default from "neo4j" to something else. This defaults to "password".
     */
    private static final String DEFAULT_ADMIN_PASSWORD = "password";

    private static final String AUTH_FORMAT = "neo4j/%s";

    private String adminPassword = DEFAULT_ADMIN_PASSWORD;

    private boolean defaultImage = false;

    /**
     * Creates a Testcontainer using the official Neo4j docker image.
     */
    public Neo4jContainer() {
        this(DOCKER_IMAGE_NAME);

        this.defaultImage = true;
    }

    /**
     * Creates a Testcontainer using a specific docker image.
     *
     * @param dockerImageName The docker image to use.
     */
    public Neo4jContainer(String dockerImageName) {
        super(dockerImageName);

        WaitStrategy waitForBolt = new LogMessageWaitStrategy()
            .withRegEx(String.format(".*Bolt enabled on 0\\.0\\.0\\.0:%d\\.\n", DEFAULT_BOLT_PORT));
        WaitStrategy waitForHttp = new HttpWaitStrategy()
            .forPort(DEFAULT_HTTP_PORT)
            .forStatusCodeMatching(response -> response == HTTP_OK);

        this.waitStrategy = new WaitAllStrategy()
            .withStrategy(waitForBolt)
            .withStrategy(waitForHttp)
            .withStartupTimeout(Duration.ofMinutes(2));
    }

    @Override
    public Set<Integer> getLivenessCheckPortNumbers() {

        return Stream.of(DEFAULT_BOLT_PORT, DEFAULT_HTTP_PORT, DEFAULT_HTTPS_PORT)
            .map(this::getMappedPort)
            .collect(toSet());
    }

    @Override
    protected void configure() {

        addExposedPorts(DEFAULT_BOLT_PORT, DEFAULT_HTTP_PORT, DEFAULT_HTTPS_PORT);

        boolean emptyAdminPassword = this.adminPassword == null || this.adminPassword.isEmpty();
        String neo4jAuth = emptyAdminPassword ? "none" : String.format(AUTH_FORMAT, this.adminPassword);
        addEnv("NEO4J_AUTH", neo4jAuth);
    }

    /**
     * @return Bolt URL for use with Neo4j's Java-Driver.
     */
    public String getBoltUrl() {
        return String.format("bolt://" + getContainerIpAddress() + ":" + getMappedPort(DEFAULT_BOLT_PORT));
    }

    /**
     * @return URL of the transactional HTTP endpoint.
     */
    public String getHttpUrl() {
        return String.format("http://" + getContainerIpAddress() + ":" + getMappedPort(DEFAULT_HTTP_PORT));
    }

    /**
     * @return URL of the transactional HTTPS endpoint.
     */
    public String getHttpsUrl() {
        return String.format("https://" + getContainerIpAddress() + ":" + getMappedPort(DEFAULT_HTTPS_PORT));
    }

    /**
     * Configures the container to use the enterprise edition of the default docker image.
     * <br><br>
     * Please use the {@link AcceptableLicense#acceptLicense()} to explicitly accept Neo4j's enterprise license and to
     * get the configured container back.
     *
     * @return An acceptable license for the enterprise edition of Neo4j.
     */
    public AcceptableLicense withEnterpriseEdition() {

        if (!defaultImage) {
            throw new IllegalStateException(
                String.format("Cannot use enterprise version with alternative image %s.", getDockerImageName()));
        }

        setDockerImageName(DOCKER_IMAGE_NAME + "-enterprise");
        return new AcceptableLicense(self());
    }

    /**
     * Sets the admin password for the default account (which is <pre>neo4j</pre>). A null value or an empty string
     * disables authentication.
     *
     * @param adminPassword The admin password for the default database account.
     * @return This container.
     */
    public S withAdminPassword(final String adminPassword) {

        this.adminPassword = adminPassword;
        return self();
    }

    /**
     * @return The admin password for the <code>neo4j</code> account or literal <code>null</code> if auth is disabled.
     */
    public String getAdminPassword() {
        return adminPassword;
    }

    /**
     * Representation of the enterprise license as an intermediate before being able to use the container.
     */
    public final static class AcceptableLicense {
        private final Neo4jContainer neo4jContainer;

        public AcceptableLicense(final Neo4jContainer neo4jContainer) {
            this.neo4jContainer = neo4jContainer;
        }

        public Neo4jContainer acceptLicense() {
            this.neo4jContainer.addEnv("NEO4J_ACCEPT_LICENSE_AGREEMENT", "yes");
            return this.neo4jContainer;
        }
    }
}
