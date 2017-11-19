package org.testcontainers.containers;

import org.testcontainers.containers.wait.HttpWaitStrategy;

import java.time.Duration;

public class ClickHouseContainer<SELF extends ClickHouseContainer<SELF>> extends JdbcDatabaseContainer<SELF> {
    public static final String NAME = "clickhouse";
    public static final String IMAGE = "yandex/clickhouse-server";
    public static final Integer HTTP_PORT = 8123;
    public static final Integer NATIVE_PORT = 9000;

    private String databaseName = "default";
    private String username = "default";
    private String password = "";

    public ClickHouseContainer() {
        super(IMAGE + ":1.1.54310");
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
        return "ru.yandex.clickhouse.ClickHouseDriver";
    }

    @Override
    public String getJdbcUrl() {
        return "jdbc:clickhouse://" + getContainerIpAddress() + ":" + getMappedPort(HTTP_PORT) + "/" + databaseName;
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

}
