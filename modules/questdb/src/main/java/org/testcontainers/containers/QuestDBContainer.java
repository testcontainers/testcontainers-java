package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

public final class QuestDBContainer extends JdbcDatabaseContainer<QuestDBContainer> {
    private static final int PGWIRE_PORT = 8812;
    private static final int ILP_PORT = 9009;
    private static final int REST_PORT = 9000;

    // commit lag 1s is more suitable for testing
    private static final int DEFAULT_COMMIT_LAG_MS = 1000;
    private static final String DEFAULT_USERNAME = "admin";
    private static final String DEFAULT_PASSWORD = "quest";
    private static final String SQL_TEST_STRING = "select * from long_sequence(1)";
    private static final String DRIVER_FQCN = "org.postgresql.Driver";

    public QuestDBContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        addExposedPort(PGWIRE_PORT);
        addExposedPort(ILP_PORT);
        addExposedPort(REST_PORT);
        withCommitLag(DEFAULT_COMMIT_LAG_MS);
        this.waitStrategy = new HttpWaitStrategy()
                .forStatusCode(200)
                .forResponsePredicate("Ok."::equals)
                .withStartupTimeout(Duration.ofMinutes(1));
    }

    public QuestDBContainer withCommitLag(int commitLagMillis) {
        addEnv("QDB_CAIRO_COMMIT_LAG", String.valueOf(commitLagMillis));
        return this;
    }

    @Override
    public String getDriverClassName() {
        return DRIVER_FQCN;
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:postgresql://" + getHost() + ":" + getMappedPort(PGWIRE_PORT) + "/";
    }

    @Override
    public String getUsername() {
        return DEFAULT_USERNAME;
    }

    @Override
    public String getPassword() {
        return DEFAULT_PASSWORD;
    }

    @Override
    protected String getTestQueryString() {
        return SQL_TEST_STRING;
    }

    public String getIlpUrl() {
        return getHost() + ":" + getMappedPort(ILP_PORT);
    }

    public String getHttpUrl() {
        return "http://" + getHost() + ":" + getMappedPort(REST_PORT);
    }
}
