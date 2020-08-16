package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

/**
 * Factory for MS SQL Server containers.
 */
public class MSSQLServerContainerProvider extends JdbcDatabaseContainerProvider {
    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(MSSQLServerContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance() {
        return newInstance(MSSQLServerContainer.DEFAULT_TAG);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        return new MSSQLServerContainer(DockerImageName.parse(MSSQLServerContainer.IMAGE).withTag(tag));
    }
}
