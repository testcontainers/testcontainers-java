package org.testcontainers.containers;

public class CockroachContainerProvider extends JdbcDatabaseContainerProvider {
    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(CockroachContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance() {
        return newInstance(CockroachContainer.IMAGE_TAG);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        return new CockroachContainer(CockroachContainer.IMAGE + ":" + tag);
    }
}
