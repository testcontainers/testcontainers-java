package org.testcontainers.clickhouse;

import lombok.Getter;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Testcontainers implementation for ClickHouse with HTTP API support.
 * <p>
 * This container provides access to ClickHouse's HTTP interface for executing queries
 * and managing the database via REST API calls.
 * <p>
 * Supported image: {@code clickhouse/clickhouse-server}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>HTTP API: 8123</li>
 *     <li>Native: 9000</li>
 * </ul>
 */
public class ClickHouseHttpContainer extends GenericContainer<ClickHouseHttpContainer> {

    static final String CLICKHOUSE_CLICKHOUSE_SERVER = "clickhouse/clickhouse-server";

    private static final DockerImageName CLICKHOUSE_IMAGE_NAME = DockerImageName.parse(CLICKHOUSE_CLICKHOUSE_SERVER);

    static final Integer HTTP_PORT = 8123;

    static final Integer NATIVE_PORT = 9000;

    static final String DEFAULT_USER = "test";

    static final String DEFAULT_PASSWORD = "test";

    @Getter
    private String databaseName = "default";

    @Getter
    private String username = DEFAULT_USER;

    @Getter
    private String password = DEFAULT_PASSWORD;

    public ClickHouseHttpContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public ClickHouseHttpContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(CLICKHOUSE_IMAGE_NAME);

        addExposedPorts(HTTP_PORT, NATIVE_PORT);
        waitingFor(
            Wait
                .forHttp("/")
                .forPort(HTTP_PORT)
                .forStatusCode(200)
                .forResponsePredicate("Ok."::equals)
                .withStartupTimeout(Duration.ofMinutes(1))
        );
    }

    @Override
    protected void configure() {
        withEnv("CLICKHOUSE_DB", this.databaseName);
        withEnv("CLICKHOUSE_USER", this.username);
        withEnv("CLICKHOUSE_PASSWORD", this.password);
    }

    /**
     * Gets the HTTP URL for the ClickHouse HTTP API.
     *
     * @return the HTTP URL
     */
    public String getHttpUrl() {
        return String.format("http://%s:%d", getHost(), getMappedPort(HTTP_PORT));
    }

    /**
     * Gets the HTTP URL with database path.
     *
     * @return the HTTP URL with database path
     */
    public String getHttpUrl(String database) {
        return String.format("http://%s:%d/?database=%s", getHost(), getMappedPort(HTTP_PORT), database);
    }

    /**
     * Gets the HTTP host and port address.
     *
     * @return the HTTP host and port
     */
    public String getHttpHostAddress() {
        return getHost() + ":" + getMappedPort(HTTP_PORT);
    }

    /**
     * Gets the mapped HTTP port.
     *
     * @return the mapped HTTP port
     */
    public Integer getHttpPort() {
        return getMappedPort(HTTP_PORT);
    }

    /**
     * Gets the mapped native port.
     *
     * @return the mapped native port
     */
    public Integer getNativePort() {
        return getMappedPort(NATIVE_PORT);
    }

    /**
     * Sets the database name.
     *
     * @param databaseName the database name
     * @return this container instance
     */
    public ClickHouseHttpContainer withDatabaseName(String databaseName) {
        this.databaseName = databaseName;
        return this;
    }

    /**
     * Sets the username.
     *
     * @param username the username
     * @return this container instance
     */
    public ClickHouseHttpContainer withUsername(String username) {
        this.username = username;
        return this;
    }

    /**
     * Sets the password.
     *
     * @param password the password
     * @return this container instance
     */
    public ClickHouseHttpContainer withPassword(String password) {
        this.password = password;
        return this;
    }
}
