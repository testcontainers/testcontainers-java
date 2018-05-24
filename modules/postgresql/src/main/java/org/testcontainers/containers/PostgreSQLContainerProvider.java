package org.testcontainers.containers;

/**
 * Factory for PostgreSQL containers.
 */
public class PostgreSQLContainerProvider extends JdbcDatabaseContainerProvider {
    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(PostgreSQLContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance() {
        return newInstance(PostgreSQLContainer.DEFAULT_TAG);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        return new PostgreSQLContainer(PostgreSQLContainer.IMAGE + ":" + tag);
    }
}
