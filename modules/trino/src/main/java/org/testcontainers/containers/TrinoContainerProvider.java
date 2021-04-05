package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

/**
 * Factory for Trino containers.
 */
public class TrinoContainerProvider extends JdbcDatabaseContainerProvider {
    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(TrinoContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance() {
        return newInstance(TrinoContainer.DEFAULT_TAG);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        return new TrinoContainer(DockerImageName.parse(TrinoContainer.IMAGE).withTag(tag));
    }
}
