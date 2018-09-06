package org.testcontainers.containers;

public class ClickHouseProvider extends JdbcDatabaseContainerProvider {
    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(ClickHouseContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        return new ClickHouseContainer(ClickHouseContainer.IMAGE + ":" + tag);
    }
}
