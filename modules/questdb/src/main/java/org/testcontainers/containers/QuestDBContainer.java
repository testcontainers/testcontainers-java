package org.testcontainers.containers;

import lombok.NonNull;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers implementation for QuestDB.
 * <p>
 * Supported image: {@code questdb/questdb}
 * <p>
 * Exposed ports:
 * <ul>
 *     <li>Postgres: 8812</li>
 *     <li>HTTP: 9000</li>
 *     <li>ILP: 9009</li>
 * </ul>
 */
public class QuestDBContainer extends JdbcDatabaseContainer<QuestDBContainer> {

    @Deprecated
    static final String LEGACY_DATABASE_PROVIDER = "postgresql";

    static final String DATABASE_PROVIDER = "questdb";

    private static final String DEFAULT_DATABASE_NAME = "qdb";

    private static final int DEFAULT_COMMIT_LAG_MS = 1000;

    private static final String DEFAULT_USERNAME = "admin";

    private static final String DEFAULT_PASSWORD = "quest";

    private static final Integer POSTGRES_PORT = 8812;

    private static final Integer REST_PORT = 9000;

    private static final Integer ILP_PORT = 9009;

    static final String TEST_QUERY = "SELECT 1";

    static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("questdb/questdb");

    public QuestDBContainer(@NonNull String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public QuestDBContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        withExposedPorts(POSTGRES_PORT, REST_PORT, ILP_PORT);
        addEnv("QDB_CAIRO_COMMIT_LAG", String.valueOf(DEFAULT_COMMIT_LAG_MS));
        waitingFor(Wait.forLogMessage("(?i).*A server-main enjoy.*", 1));
    }

    @Override
    public String getDriverClassName() {
        return "org.postgresql.Driver";
    }

    @Override
    public String getJdbcUrl() {
        return String.format("jdbc:postgresql://%s:%d/%s", getHost(), getMappedPort(8812), getDefaultDatabaseName());
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
    public String getTestQueryString() {
        return TEST_QUERY;
    }

    @Override
    protected void waitUntilContainerStarted() {
        getWaitStrategy().waitUntilReady(this);
    }

    public String getDefaultDatabaseName() {
        return DEFAULT_DATABASE_NAME;
    }

    public String getIlpUrl() {
        return getHost() + ":" + getMappedPort(ILP_PORT);
    }

    public String getHttpUrl() {
        return "http://" + getHost() + ":" + getMappedPort(REST_PORT);
    }
}
