package org.testcontainers.containers;

import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.utility.DockerImageName;

public class ClickHouseProvider extends JdbcDatabaseContainerProvider {

    private static final String DEFAULT_TAG = "24.12-alpine";

    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals("clickhouse");
    }

    @Override
    public JdbcDatabaseContainer<?> newInstance() {
        return newInstance(DEFAULT_TAG);
    }

    @Override
    public JdbcDatabaseContainer<?> newInstance(String tag) {
        return new ClickHouseContainer(DockerImageName.parse("clickhouse/clickhouse-server").withTag(tag));
    }
}
