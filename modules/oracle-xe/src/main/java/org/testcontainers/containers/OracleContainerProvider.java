package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

/**
 * Factory for Oracle containers.
 */
public class OracleContainerProvider extends JdbcDatabaseContainerProvider<OracleContainer> {
    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(OracleContainer.NAME);
    }

    @Override
    public OracleContainer newInstance() {
        return newInstance(OracleContainer.DEFAULT_TAG);
    }

    @Override
    public OracleContainer newInstance(String tag) {
        if (tag != null) {
            return new OracleContainer(DockerImageName.parse(OracleContainer.IMAGE).withTag(tag));
        }
        return newInstance();
    }
}
