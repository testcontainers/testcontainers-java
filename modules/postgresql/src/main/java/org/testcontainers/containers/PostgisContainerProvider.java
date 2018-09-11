package org.testcontainers.containers;

/**
 * Factory for Postgis containers.
 */
public class PostgisContainerProvider extends JdbcDatabaseContainerProvider {
    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(PostgisContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance() {
        return newInstance(PostgisContainer.DEFAULT_TAG);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        return new PostgisContainer(PostgisContainer.IMAGE + ":" + tag);
    }
}
