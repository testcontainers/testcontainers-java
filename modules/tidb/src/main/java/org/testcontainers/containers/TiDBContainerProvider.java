package org.testcontainers.containers;

/**
 * Factory for TiDB containers.
 */
public class TiDBContainerProvider extends JdbcDatabaseContainerProvider {
    private static final String DEFAULT_TAG = "v6.1.0";

    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(TiDBContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance() {
        return newInstance(DEFAULT_TAG);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        if (tag != null) {
            return new TiDBContainer(tag);
        } else {
            return newInstance();
        }
    }
}
