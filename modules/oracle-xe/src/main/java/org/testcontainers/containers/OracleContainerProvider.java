package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

/**
 * Factory for Oracle containers.
 */
public class OracleContainerProvider extends JdbcDatabaseContainerProvider {
    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(OracleContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance() {
        return newInstance(OracleContainer.DEFAULT_TAG);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        if (tag != null) {
            return new OracleContainer(DockerImageName.parse(OracleContainer.IMAGE).withTag(tag));
        }
        return newInstance();
    }
}
