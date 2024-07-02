package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

public class TimeplusProvider extends JdbcDatabaseContainerProvider {

    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(TimeplusContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        return new TimeplusContainer(DockerImageName.parse(TimeplusContainer.IMAGE).withTag(tag));
    }
}
