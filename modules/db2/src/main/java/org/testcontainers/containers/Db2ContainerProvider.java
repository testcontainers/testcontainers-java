package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

public class Db2ContainerProvider extends JdbcDatabaseContainerProvider {
    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(Db2Container.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance() {
        return newInstance(Db2Container.DEFAULT_TAG);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        return new Db2Container(DockerImageName.parse(Db2Container.DEFAULT_DB2_IMAGE_NAME).withTag(tag));
    }
}
