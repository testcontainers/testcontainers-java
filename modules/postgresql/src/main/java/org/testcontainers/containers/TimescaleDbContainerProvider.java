package org.testcontainers.containers;

import org.testcontainers.jdbc.ConnectionUrl;

/**
 * Factory for TimescaleDb containers, which are a special flavor of PostgreSQL.
 */
public class TimescaleDbContainerProvider extends JdbcDatabaseContainerProvider {

    private static final String NAME = "timescaledb";
    private static final String DEFAULT_TAG = "latest-pg11";
    private static final String DEFAULT_IMAGE = "timescale/timescaledb";
    public static final String USER_PARAM = "user";
    public static final String PASSWORD_PARAM = "password";


    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(NAME);
    }

    @Override
    public PostgreSQLContainer<?> newInstance() {
        return newInstance(DEFAULT_TAG);
    }

    @Override
    public PostgreSQLContainer<?> newInstance(String tag) {
        return new PostgreSQLContainer(DEFAULT_IMAGE + ":" + tag);
    }

    @Override
    public JdbcDatabaseContainer<?> newInstance(ConnectionUrl connectionUrl) {
        return newInstanceFromConnectionUrl(connectionUrl, USER_PARAM, PASSWORD_PARAM);
    }
}
