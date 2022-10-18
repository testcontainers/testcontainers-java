package org.testcontainers.containers;

import lombok.NonNull;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

public class QuestDBContainer extends JdbcDatabaseContainer<QuestDBContainer> {

    public static final String DATABASE_PROVIDER = "postgresql";

    private static final String DEFAULT_TAG = "6.5.3";

    private static final String DEFAULT_DATABASE_NAME = "qdb";

    private static final String DEFAULT_USERNAME = "admin";

    private static final String DEFAULT_PASSWORD = "quest";

    public static final Integer POSTGRES_PORT = 8812;

    public static final Integer REST_PORT = 9000;

    public static final Integer ILP_PORT = 9009;

    public static final String TEST_QUERY = "SELECT 1";

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("questdb/questdb");

    @Deprecated
    public static final String IMAGE = DEFAULT_IMAGE_NAME.getUnversionedPart();

    @Deprecated
    public QuestDBContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    public QuestDBContainer(@NonNull String dockerImageName) {
        super(dockerImageName);
    }

    public QuestDBContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        this.withExposedPorts(POSTGRES_PORT, REST_PORT, ILP_PORT);
        this.waitingFor(Wait.forLogMessage("(?i).*A server-main enjoy.*", 1));
    }

    @Override
    public String getDriverClassName() {
        return "org.postgresql.Driver";
    }

    @Override
    public String getJdbcUrl() {
        return this.getPostgresUrl(DEFAULT_DATABASE_NAME);
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

    public Integer getIlpPort() {
        return getMappedPort(ILP_PORT);
    }

    public String getPostgresUrl(String databaseName) {
        if (!this.isRunning()) {
            throw new IllegalStateException("QuestDBContainer should be started first");
        } else {
            return this.getPostgresConnectionString() + "/" + databaseName;
        }
    }

    public String getPostgresConnectionString() {
        return String.format("jdbc:postgresql://%s:%d", this.getHost(), this.getMappedPort(8812));
    }
}
