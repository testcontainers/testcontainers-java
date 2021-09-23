package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

public class Db2ContainerProvider extends JdbcDatabaseContainerProvider<Db2Container> {
    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(Db2Container.NAME);
    }

    @Override
    public Db2Container newInstance() {
        return newInstance(Db2Container.DEFAULT_TAG);
    }

    @Override
    public Db2Container newInstance(String tag) {
        return new Db2Container(DockerImageName.parse(Db2Container.DEFAULT_DB2_IMAGE_NAME).withTag(tag));
    }
}
