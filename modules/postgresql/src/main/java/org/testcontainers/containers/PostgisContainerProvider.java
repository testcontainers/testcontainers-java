package org.testcontainers.containers;

import org.testcontainers.jdbc.ConnectionUrl;
import org.testcontainers.utility.DockerImageName;

/**
 * Factory for PostGIS containers, which are a special flavour of PostgreSQL.
 */
public class PostgisContainerProvider extends JdbcDatabaseContainerProvider {

    private static final String NAME = "postgis";
    private static final String DEFAULT_TAG = "12-3.0";
    private static final String DEFAULT_IMAGE = "postgis/postgis";
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
        return new PostgreSQLContainer(DockerImageName.parse(DEFAULT_IMAGE).withTag(DEFAULT_TAG));
    }

    @Override
    public JdbcDatabaseContainer newInstance(ConnectionUrl connectionUrl) {
        return newInstanceFromConnectionUrl(connectionUrl, USER_PARAM, PASSWORD_PARAM);
    }
}
