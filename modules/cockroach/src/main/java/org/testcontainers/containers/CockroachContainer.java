package org.testcontainers.containers;

import org.testcontainers.containers.wait.HttpWaitStrategy;

import java.time.Duration;

public class CockroachContainer<SELF extends CockroachContainer<SELF>> extends JdbcDatabaseContainer<SELF> {
    public static final String NAME = "cockroach";
    public static final String IMAGE = "cockroachdb/cockroach";
    public static final int REST_API_PORT = 8080;
    public static final int DB_PORT = 26257;

    private String databaseName = "test";
    private String username = "root";
    private String password = "";

    public CockroachContainer() {
        this(IMAGE + ":v1.1.2");
    }

    public CockroachContainer(final String dockerImageName) {
        super(dockerImageName);
    }

    @Override
    protected Integer getLivenessCheckPort() {
        return getMappedPort(REST_API_PORT);
    }

    @Override
    protected void configure() {
        withExposedPorts(REST_API_PORT, DB_PORT);
        withEnv("COCKROACH_USER", username);
        withEnv("COCKROACH_DATABASE", databaseName);
        withExposedPorts(REST_API_PORT, DB_PORT);
        waitingFor(
                new HttpWaitStrategy()
                        .forPath("/health")
                        .forStatusCode(200)
                        .withStartupTimeout(Duration.ofMinutes(1))
        );
        withCommand("start --insecure");
    }

    @Override
    public String getDriverClassName() {
        return "org.postgresql.Driver";
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:postgresql://" + getContainerIpAddress() + ":" + getMappedPort(DB_PORT) + "/" + databaseName;
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
    public String getTestQueryString() {
        return "SELECT 1";
    }

    @Override
    public SELF withDatabaseName(final String databaseName) {
        this.databaseName = databaseName;
        return self();
    }

    @Override
    public SELF withUsername(String username) {
        this.username = username;
        return self();
    }

    @Override
    protected void waitUntilContainerStarted() {
        getWaitStrategy().waitUntilReady(this);
    }
}
