package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

/**
 * Factory for OceanBase containers.
 */
public class OceanBaseContainerProvider extends JdbcDatabaseContainerProvider {

    private static final String DEFAULT_TAG = "4.2.1_bp3";

    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(OceanBaseContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance() {
        return newInstance(DEFAULT_TAG);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        if (tag != null) {
            return new OceanBaseContainer(DockerImageName.parse(OceanBaseContainer.DOCKER_IMAGE_NAME).withTag(tag));
        } else {
            return newInstance();
        }
    }
}
