package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;

import java.time.Duration;

public class CockroachContainer extends JdbcDatabaseContainer<CockroachContainer> {
    public static final String NAME = "cockroach";
    public static final String IMAGE = "cockroachdb/cockroach";
    public static final String IMAGE_TAG = "v2.1.4";
    public static final String JDBC_DRIVER_CLASS_NAME = "org.postgresql.Driver";
    public static final String JDBC_URL_PREFIX = "jdbc:postgresql";
    public static final String TEST_QUERY_STRING = "SELECT 1";
    public static final int REST_API_PORT = 8080;
    public static final int DB_PORT = 26257;

    private String databaseName = "test";
    private String username = "root";
    private String password = "";

    public CockroachContainer() {
        this(IMAGE + ":" + IMAGE_TAG);
    }

    public CockroachContainer(final String dockerImageName) {
        super(dockerImageName);
    }

    @Override
    protected void configure() {
        withExposedPorts(REST_API_PORT, DB_PORT);
        withEnv("COCKROACH_USER", username);
        withEnv("COCKROACH_DATABASE", databaseName);
        waitingFor(
            new HttpWaitStrategy()
                .forPath("/health")
                .forPort(REST_API_PORT)
                .forStatusCode(200)
                .withStartupTimeout(Duration.ofMinutes(1))
        );
        withCommand("start --insecure");
    }

    @Override
    public String getDriverClassName() {
        return JDBC_DRIVER_CLASS_NAME;
    }

    @Override
    public String getJdbcUrl() {
        return JDBC_URL_PREFIX + "://" + getContainerIpAddress() + ":" + getMappedPort(DB_PORT) + "/" + databaseName;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public CockroachContainer withPassword(String password) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getTestQueryString() {
        return TEST_QUERY_STRING;
    }

    @Override
    public CockroachContainer withDatabaseName(final String databaseName) {
        this.databaseName = databaseName;
        return self();
    }

    @Override
    public CockroachContainer withUsername(String username) {
        this.username = username;
        return self();
    }

}
