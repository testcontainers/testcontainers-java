package org.testcontainers.containers;

import org.testcontainers.utility.DockerImageName;

public class CockroachContainerProvider extends JdbcDatabaseContainerProvider {
    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(CockroachContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance() {
        return newInstance(CockroachContainer.IMAGE_TAG);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        return new CockroachContainer(DockerImageName.parse(CockroachContainer.IMAGE).withTag(tag));
    }
}
