package org.testcontainers.containers;

/**
 * Factory for MariaDB org.testcontainers.containers.
 */
public class MariaDBContainerProvider extends JdbcDatabaseContainerProvider {
    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(MariaDBContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance() {
        return newInstance(MariaDBContainer.DEFAULT_TAG);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        return new MariaDBContainer(MariaDBContainer.IMAGE + ":" + tag);
    }
}
