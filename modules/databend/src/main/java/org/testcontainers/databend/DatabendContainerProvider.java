package org.testcontainers.databend;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.JdbcDatabaseContainerProvider;

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
            return new DatabendContainer(DatabendContainer.DOCKER_IMAGE_NAME.withTag(tag));
        } else {
            return newInstance();
        }
    }
}
