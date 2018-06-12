package org.testcontainers.containers;

/**
 * Factory for Sybase containers.
 */
public class SybaseContainerProvider extends JdbcDatabaseContainerProvider {
    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(SybaseContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance() {
        return newInstance(SybaseContainer.DEFAULT_TAG);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        return new SybaseContainer(SybaseContainer.IMAGE + ":" + tag);
    }
}
