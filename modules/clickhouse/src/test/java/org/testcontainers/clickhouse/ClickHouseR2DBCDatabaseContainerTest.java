package org.testcontainers.clickhouse;

import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.r2dbc.AbstractR2DBCDatabaseContainerTest;

public class ClickHouseR2DBCDatabaseContainerTest extends AbstractR2DBCDatabaseContainerTest<ClickHouseContainer> {

    @Override
    protected ConnectionFactoryOptions getOptions(ClickHouseContainer container) {
        return ClickHouseR2DBCDatabaseContainer.getOptions(container);
    }

    @Override
    protected String createR2DBCUrl() {
        return "r2dbc:tc:clickhouse:///db?TC_IMAGE_TAG=21.11.11-alpine";
    }

    @Override
    protected ClickHouseContainer createContainer() {
        return new ClickHouseContainer("clickhouse/clickhouse-server:21.11.11-alpine");
    }
}
