package org.testcontainers.clickhouse;

import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.r2dbc.AbstractR2DBCDatabaseContainerTest;

public class ClickHouseR2DBCDatabaseContainerTest extends AbstractR2DBCDatabaseContainerTest<ClickHouseContainer> {
    @Override
    protected ConnectionFactoryOptions getOptions(ClickHouseContainer container) {
        // spotless:off
        // get_options {
        ConnectionFactoryOptions options = ClickHouseR2DBCDatabaseContainer.getOptions(
            container
        );
        // }
        // spotless:on

        return options;
    }

    @Override
    protected String createR2DBCUrl() {
        return "r2dbc:tc:clickhouse:///db?TC_IMAGE_TAG=21.9.2-alpine";
    }

    @Override
    protected ClickHouseContainer createContainer() {
        return new ClickHouseContainer("clickhouse/clickhouse-server:21.9.2-alpine")
            .withExposedPorts(ClickHouseContainer.HTTP_PORT, ClickHouseContainer.NATIVE_PORT)
            .withUsername("test")
            .withPassword("test")
            .withDatabaseName("db");
    }
}
