package org.testcontainers.containers;

import org.testcontainers.jdbc.ConnectionUrl;

import java.util.Objects;

/**
 * Factory for pgroonga container, which is PostgreSQL with groonga extension.
 * PGroonga makes PostgreSQL fast full text search platform for all languages!
 */
public class PgroongaContainerProvider extends JdbcDatabaseContainerProvider {

    private static final String NAME = "pgroonga";
    private static final String DEFAULT_TAG = "2.2.1-alpine-11-slim";
    private static final String DEFAULT_IMAGE = "groonga/pgroonga";
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
