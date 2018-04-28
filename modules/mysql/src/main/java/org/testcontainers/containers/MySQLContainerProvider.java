package org.testcontainers.containers;

import java.util.Optional;

/**
 * Factory for MySQL containers.
 */
public class MySQLContainerProvider extends JdbcDatabaseContainerProvider {
    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(MySQLContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance(Optional<String> tag) {
        return new MySQLContainer(MySQLContainer.IMAGE + ":" + tag.orElse(MySQLContainer.DEFAULT_TAG));
    }
}
