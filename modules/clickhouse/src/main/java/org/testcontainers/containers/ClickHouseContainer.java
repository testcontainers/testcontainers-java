package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

/**
 * Testcontainers implementation for ClickHouse.
 *
 */
public class ClickHouseContainer extends JdbcDatabaseContainer<ClickHouseContainer> {

    public static final String NAME = "clickhouse";

    private static final DockerImageName CLICKHOUSE_IMAGE_NAME = DockerImageName.parse("clickhouse/clickhouse-server");

    public static final String IMAGE = CLICKHOUSE_IMAGE_NAME.getUnversionedPart();

    public static final Integer HTTP_PORT = 8123;

    public static final Integer NATIVE_PORT = 9000;

    private static final String DRIVER_CLASS_NAME = "com.clickhouse.jdbc.ClickHouseDriver";

    private static final String JDBC_URL_PREFIX = "jdbc:" + NAME + "://";

    private static final String TEST_QUERY = "SELECT 1";

    private String databaseName = "default";

    private String username = "default";

    private String password = "";

    public ClickHouseContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public ClickHouseContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(CLICKHOUSE_IMAGE_NAME, CLICKHOUSE_IMAGE_NAME);

        addExposedPorts(HTTP_PORT, NATIVE_PORT);
        this.waitStrategy =
            new HttpWaitStrategy()
                .forStatusCode(200)
                .forResponsePredicate("Ok."::equals)
                .withStartupTimeout(Duration.ofMinutes(1));
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
            throw new RuntimeException("Could not load ClickHouse JDBC driver", e);
        }
    }

    @Override
    public String getJdbcUrl() {
        return JDBC_URL_PREFIX + getHost() + ":" + getMappedPort(HTTP_PORT) + "/" + databaseName;
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
        return TEST_QUERY;
    }

    @Override
    public ClickHouseContainer withUrlParam(String paramName, String paramValue) {
        throw new UnsupportedOperationException("The ClickHouse does not support this");
    }
}
