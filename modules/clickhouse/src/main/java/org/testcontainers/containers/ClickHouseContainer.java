package org.testcontainers.containers;

import org.testcontainers.containers.wait.HttpWaitStrategy;

import java.time.Duration;

public class ClickHouseContainer extends JdbcDatabaseContainer {
    public static final String NAME = "clickhouse";
    public static final String IMAGE = "yandex/clickhouse-server";
    public static final String DEFAULT_TAG = "18.10.3";

    public static final Integer HTTP_PORT = 8123;
    public static final Integer NATIVE_PORT = 9000;

    private static final String DRIVER_CLASS_NAME = "ru.yandex.clickhouse.ClickHouseDriver";
    private static final String JDBC_URL_PREFIX = "jdbc:" + NAME + "://";
    private static final String TEST_QUERY = "SELECT 1";

    private String databaseName = "default";
    private String username = "default";
    private String password = "";

    public ClickHouseContainer() {
        super(IMAGE + ":" + DEFAULT_TAG);
    }

    public ClickHouseContainer(String dockerImageName) {
        super(dockerImageName);
    }

    @Override
    protected void configure() {
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
        return JDBC_URL_PREFIX + getContainerIpAddress() + ":" + getMappedPort(HTTP_PORT) + "/" + databaseName;
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

}
