package org.testcontainers.containers;

import org.testcontainers.jdbc.ConnectionUrl;

/**
 * Factory for PgRouting container.
 */
public class PgroutingContainerProvider extends JdbcDatabaseContainerProvider {

    private static final String NAME = "pgrouting";
    private static final String DEFAULT_TAG = "11-2.5-2.6";
    private static final String DEFAULT_IMAGE = "konturio/pgrouting";
    public static final String USER_PARAM = "user";
    public static final String PASSWORD_PARAM = "password";


    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance() {
        return newInstance(DEFAULT_TAG);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        return new PostgreSQLContainer(DEFAULT_IMAGE + ":" + tag);
    }

    @Override
    public JdbcDatabaseContainer newInstance(ConnectionUrl connectionUrl) {
        return newInstanceFromConnectionUrl(connectionUrl, USER_PARAM, PASSWORD_PARAM);
    }
}
