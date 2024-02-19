package org.testcontainers.oceanbase;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.JdbcDatabaseContainerProvider;
import org.testcontainers.utility.DockerImageName;

/**
 * Factory for OceanBase Community Edition containers.
 */
public class OceanBaseCEContainerProvider extends JdbcDatabaseContainerProvider {

    private static final String DEFAULT_TAG = "4.2.1_bp3";

    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(OceanBaseCEContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance() {
        return newInstance(DEFAULT_TAG);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        if (tag != null) {
            return new OceanBaseCEContainer(DockerImageName.parse(OceanBaseCEContainer.DOCKER_IMAGE_NAME).withTag(tag));
        } else {
            return newInstance();
        }
    }
}
