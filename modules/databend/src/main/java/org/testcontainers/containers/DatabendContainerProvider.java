package org.testcontainers.containers;

import org.testcontainers.databend.DatabendContainer;
import org.testcontainers.utility.DockerImageName;

public class DatabendContainerProvider extends JdbcDatabaseContainerProvider {

    private static final String DEFAULT_TAG = "v1.2.615";

    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(DatabendContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance() {
        return newInstance(DEFAULT_TAG);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        if (tag != null) {
            return new DatabendContainer(DockerImageName.parse(DatabendContainer.DOCKER_IMAGE_NAME).withTag(tag));
        } else {
            return newInstance();
        }
    }
}
