package org.testcontainers.containers;

import java.util.Optional;

/**
 * Factory for PostgreSQL containers.
 */
public class PostgreSQLContainerProvider extends JdbcDatabaseContainerProvider {
    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(PostgreSQLContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance(Optional<String> tag) {
        return new PostgreSQLContainer(PostgreSQLContainer.IMAGE + ":" + tag.orElse(PostgreSQLContainer.DEFAULT_TAG));
    }
}
