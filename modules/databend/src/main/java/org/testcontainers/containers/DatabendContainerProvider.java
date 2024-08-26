package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

public class DatabendContainerProvider extends JdbcDatabaseContainerProvider {

    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(DatabendContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        return new DatabendContainer(DockerImageName.parse(DatabendContainer.IMAGE).withTag(tag));
    }
}
