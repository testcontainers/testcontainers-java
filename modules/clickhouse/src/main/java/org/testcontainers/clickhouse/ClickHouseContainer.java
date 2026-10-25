package org.testcontainers.clickhouse;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * Testcontainers implementation for ClickHouse.
 * <p>
 * Supported image: {@code clickhouse/clickhouse-server}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>Database: 8123</li>
 *     <li>Console: 9000</li>
 *     <li>MySQL: 9004</li>
 *     <li>PostgreSQL: 9005</li>
 * </ul>
 */
public class ClickHouseContainer extends JdbcDatabaseContainer<ClickHouseContainer> {

    static final String CLICKHOUSE_CLICKHOUSE_SERVER = "clickhouse/clickhouse-server";

    private static final DockerImageName CLICKHOUSE_IMAGE_NAME = DockerImageName.parse(CLICKHOUSE_CLICKHOUSE_SERVER);

    static final Integer HTTP_PORT = 8123;

    static final Integer NATIVE_PORT = 9000;

    static final Integer MYSQL_PORT = 9004;

    static final Integer POSTGRESQL_PORT = 9005;

    private static final String LEGACY_V1_DRIVER_CLASS_NAME = "com.clickhouse.jdbc.ClickHouseDriver";

    private static final String DRIVER_CLASS_NAME = "com.clickhouse.jdbc.Driver";

    private static final String JDBC_URL_PREFIX = "jdbc:clickhouse://";

    private static final String TEST_QUERY = "SELECT 1";

    static final String DEFAULT_USER = "test";

    static final String DEFAULT_PASSWORD = "test";

    private String databaseName = "default";

    private String username = DEFAULT_USER;

    private String password = DEFAULT_PASSWORD;

    public ClickHouseContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public ClickHouseContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(CLICKHOUSE_IMAGE_NAME);

        addExposedPorts(HTTP_PORT, NATIVE_PORT, MYSQL_PORT, POSTGRESQL_PORT);
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

    @Override
    public Set<Integer> getLivenessCheckPortNumbers() {
        return new HashSet<>(getMappedPort(HTTP_PORT));
    }

    @Override
    public String getDriverClassName() {
        try {
            Class.forName(DRIVER_CLASS_NAME);
            return DRIVER_CLASS_NAME;
        } catch (ClassNotFoundException e) {
            return LEGACY_V1_DRIVER_CLASS_NAME;
        }
    }

    @Override
    public String getJdbcUrl() {
        return (
            JDBC_URL_PREFIX +
            getHost() +
            ":" +
            getMappedPort(HTTP_PORT) +
            "/" +
            this.databaseName +
            constructUrlParameters("?", "&")
        );
    }

    public String getHttpUrl() {
        return "http://" + getHost() + ":" + getMappedPort(HTTP_PORT);
    }

    public String getMysqlJdbcUrl() {
        return (
            "jdbc:mysql://" +
            getHost() +
            ":" +
            getMappedPort(MYSQL_PORT) +
            "/" +
            this.databaseName +
            constructUrlParameters("?", "&")
        );
    }

    public Integer getHttpPort() {
        return getMappedPort(HTTP_PORT);
    }

    public Integer getNativePort() {
        return getMappedPort(NATIVE_PORT);
    }

    public Integer getMysqlPort() {
        return getMappedPort(MYSQL_PORT);
    }

    public Integer getPostgresqlPort() {
        return getMappedPort(POSTGRESQL_PORT);
    }

    public String getPostgresqlJdbcUrl() {
        return (
            "jdbc:postgresql://" +
            getHost() +
            ":" +
            getMappedPort(POSTGRESQL_PORT) +
            "/" +
            this.databaseName +
            constructUrlParameters("?", "&")
        );
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getDatabaseName() {
        return databaseName;
    }

    @Override
    public String getTestQueryString() {
        return TEST_QUERY;
    }

    @Override
    public ClickHouseContainer withUsername(String username) {
        this.username = username;
        return this;
    }

    @Override
    public ClickHouseContainer withPassword(String password) {
        this.password = password;
        return this;
    }

    @Override
    public ClickHouseContainer withDatabaseName(String databaseName) {
        this.databaseName = databaseName;
        return this;
    }

    @Override
    protected void waitUntilContainerStarted() {
        getWaitStrategy().waitUntilReady(this);
    }
}
