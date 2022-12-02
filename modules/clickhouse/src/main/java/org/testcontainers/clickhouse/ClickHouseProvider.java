package org.testcontainers.clickhouse;

import org.testcontainers.jdbc.containers.JdbcDatabaseContainer;
import org.testcontainers.jdbc.containers.JdbcDatabaseContainerProvider;
import org.testcontainers.utility.DockerImageName;

public class ClickHouseProvider extends JdbcDatabaseContainerProvider {

    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(ClickHouseContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        return new ClickHouseContainer(DockerImageName.parse(ClickHouseContainer.IMAGE).withTag(tag));
    }
}
