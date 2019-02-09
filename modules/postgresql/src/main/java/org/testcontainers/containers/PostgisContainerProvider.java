package org.testcontainers.containers;

/**
 * Factory for PostGIS containers, which are a special flavour of PostgreSQL.
 */
public class PostgisContainerProvider extends JdbcDatabaseContainerProvider {

    private static final String NAME = "postgis";
    private static final String DEFAULT_TAG = "10";
    private static final String DEFAULT_IMAGE = "mdillon/postgis";

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
}
