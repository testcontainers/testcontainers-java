package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

/**
 * Factory for MS SQL Server containers.
 */
public class MSSQLServerContainerProvider extends JdbcDatabaseContainerProvider<MSSQLServerContainer> {
    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(MSSQLServerContainer.NAME);
    }

    @Override
    public MSSQLServerContainer newInstance() {
        return newInstance(MSSQLServerContainer.DEFAULT_TAG);
    }

    @Override
    public MSSQLServerContainer newInstance(String tag) {
        return new MSSQLServerContainer(DockerImageName.parse(MSSQLServerContainer.IMAGE).withTag(tag));
    }
}
