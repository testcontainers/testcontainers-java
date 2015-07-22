package org.testcontainers.containers;

/**
 * Created by rnorth on 21/07/2015.
 */
public class PostgreSQLContainerProvider extends JdbcDatabaseContainerProvider {
    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(PostgreSQLContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        return new PostgreSQLContainer(PostgreSQLContainer.IMAGE + ":" + tag);
    }
}
