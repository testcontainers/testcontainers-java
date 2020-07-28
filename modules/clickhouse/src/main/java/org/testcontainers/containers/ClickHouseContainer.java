package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

public class ClickHouseContainer extends JdbcDatabaseContainer {
    public static final String NAME = "clickhouse";
    public static final String IMAGE = "yandex/clickhouse-server";
    @Deprecated
    public static final String DEFAULT_TAG = "18.10.3";

    public static final Integer HTTP_PORT = 8123;
    public static final Integer NATIVE_PORT = 9000;

    private static final String DRIVER_CLASS_NAME = "ru.yandex.clickhouse.ClickHouseDriver";
    private static final String JDBC_URL_PREFIX = "jdbc:" + NAME + "://";
    private static final String TEST_QUERY = "SELECT 1";

    private String databaseName = "default";
    private String username = "default";
    private String password = "";

    /**
     * @deprecated use {@link ClickHouseContainer(DockerImageName)} instead
     */
    @Deprecated
    public ClickHouseContainer() {
        super(IMAGE + ":" + DEFAULT_TAG);
    }

    /**
     * @deprecated use {@link ClickHouseContainer(DockerImageName)} instead
     */
    @Deprecated
    public ClickHouseContainer(String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public ClickHouseContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);

        withExposedPorts(HTTP_PORT, NATIVE_PORT);
        waitingFor(
            new HttpWaitStrategy()
                .forStatusCode(200)
                .forResponsePredicate(responseBody -> "Ok.".equals(responseBody))
                .withStartupTimeout(Duration.ofMinutes(1))
        );
    }

    @Override
    protected Integer getLivenessCheckPort() {
        return getMappedPort(HTTP_PORT);
    }

    @Override
    public String getDriverClassName() {
        return DRIVER_CLASS_NAME;
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
