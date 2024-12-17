package org.testcontainers.clickhouse;

import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.r2dbc.R2DBCDatabaseContainer;

/**
 * ClickHouse R2DBC support
 */
public class ClickHouseR2DBCDatabaseContainer implements R2DBCDatabaseContainer {

    private final ClickHouseContainer container;

    public ClickHouseR2DBCDatabaseContainer(ClickHouseContainer container) {
        this.container = container;
    }

    public static ConnectionFactoryOptions getOptions(ClickHouseContainer container) {
        ConnectionFactoryOptions options = ConnectionFactoryOptions
            .builder()
            .option(ConnectionFactoryOptions.DRIVER, ClickHouseR2DBCDatabaseContainerProvider.DRIVER)
            .build();

        return new ClickHouseR2DBCDatabaseContainer(container).configure(options);
    }

    @Override
    public void start() {
        this.container.start();
    }

    @Override
    public void stop() {
        this.container.stop();
    }

    @Override
    public ConnectionFactoryOptions configure(ConnectionFactoryOptions options) {
        return options
            .mutate()
            .option(ConnectionFactoryOptions.HOST, container.getHost())
            .option(ConnectionFactoryOptions.PORT, container.getMappedPort(ClickHouseContainer.HTTP_PORT))
            .option(ConnectionFactoryOptions.DATABASE, container.getDatabaseName())
            .option(ConnectionFactoryOptions.USER, container.getUsername())
            .option(ConnectionFactoryOptions.PASSWORD, container.getPassword())
            .option(ConnectionFactoryOptions.PROTOCOL, "http")
            .build();
    }
}
