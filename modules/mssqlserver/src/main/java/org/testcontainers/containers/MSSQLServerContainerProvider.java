package org.testcontainers.containers;

import java.util.Optional;

/**
 * Factory for MS SQL Server containers.
 */
public class MSSQLServerContainerProvider extends JdbcDatabaseContainerProvider {
    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(MSSQLServerContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance(Optional<String> tag) {
        return new MSSQLServerContainer(MSSQLServerContainer.IMAGE + ":" + tag.orElse(MSSQLServerContainer.DEFAULT_TAG));
    }
}
