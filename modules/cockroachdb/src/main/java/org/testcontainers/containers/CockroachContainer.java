package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

public class CockroachContainer extends JdbcDatabaseContainer<CockroachContainer> {

    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("cockroachdb/cockroach");
    private static final String DEFAULT_TAG = "v19.2.11";

    public static final String NAME = "cockroach";

    @Deprecated
    public static final String IMAGE = DEFAULT_IMAGE_NAME.getUnversionedPart();

    @Deprecated
    public static final String IMAGE_TAG = DEFAULT_TAG;

    private static final String JDBC_DRIVER_CLASS_NAME = "org.postgresql.Driver";
    private static final String JDBC_URL_PREFIX = "jdbc:postgresql";
    private static final String TEST_QUERY_STRING = "SELECT 1";
    private static final int REST_API_PORT = 8080;
    private static final int DB_PORT = 26257;

    private String databaseName = "postgres";
    private String username = "root";
    private String password = "";

    /**
     * @deprecated use {@link CockroachContainer(DockerImageName)} instead
     */
    @Deprecated
    public CockroachContainer() {
        this(DEFAULT_IMAGE_NAME.withTag(DEFAULT_TAG));
    }

    public CockroachContainer(final String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public CockroachContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);

        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);

        withExposedPorts(REST_API_PORT, DB_PORT);
        waitingFor(
            new HttpWaitStrategy()
                .forPath("/health")
                .forPort(REST_API_PORT)
                .forStatusCode(200)
                .withStartupTimeout(Duration.ofMinutes(1))
        );
        withCommand("start-single-node --insecure");
    }

    @Override
    public String getDriverClassName() {
        return JDBC_DRIVER_CLASS_NAME;
    }

    @Override
    public String getJdbcUrl() {
        String additionalUrlParams = constructUrlParameters("?", "&");
        return JDBC_URL_PREFIX + "://" + getHost() + ":" + getMappedPort(DB_PORT) +
            "/" + databaseName + additionalUrlParams;
    }

    @Override
    public final String getDatabaseName() {
        return databaseName;
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
        return TEST_QUERY_STRING;
    }

    @Override
    public CockroachContainer withUsername(String username) {
        throw new UnsupportedOperationException("The CockroachDB docker image does not currently support this - please see https://github.com/cockroachdb/cockroach/issues/19826");
    }

    @Override
    public CockroachContainer withPassword(String password) {
        throw new UnsupportedOperationException("The CockroachDB docker image does not currently support this - please see https://github.com/cockroachdb/cockroach/issues/19826");
    }

    @Override
    public CockroachContainer withDatabaseName(final String databaseName) {
        throw new UnsupportedOperationException("The CockroachDB docker image does not currently support this - please see https://github.com/cockroachdb/cockroach/issues/19826");
    }
}
