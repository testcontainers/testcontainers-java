package org.testcontainers.tidb;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.JdbcDatabaseContainerProvider;
import org.testcontainers.utility.DockerImageName;

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
            return new TiDBContainer(DockerImageName.parse(TiDBContainer.DOCKER_IMAGE_NAME).withTag(tag));
        } else {
            return newInstance();
        }
    }
}
