package org.testcontainers.timeplus;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.JdbcDatabaseContainerProvider;
import org.testcontainers.utility.DockerImageName;

/**
 * Factory for Timeplus containers.
 */
public class TimeplusContainerProvider extends JdbcDatabaseContainerProvider {

    private static final String DEFAULT_TAG = "2.3.3";

    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(TimeplusContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance() {
        return newInstance(DEFAULT_TAG);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        if (tag != null) {
            return new TimeplusContainer(DockerImageName.parse(TimeplusContainer.DOCKER_IMAGE_NAME).withTag(tag));
        } else {
            return newInstance();
        }
    }
}
