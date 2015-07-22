package org.testcontainers.containers;

/**
 * Created by rnorth on 21/07/2015.
 */
public class MySQLContainerProvider extends JdbcDatabaseContainerProvider {
    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(MySQLContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        return new MySQLContainer(MySQLContainer.IMAGE + ":" + tag);
    }
}
