package org.testcontainers.containers;

import io.r2dbc.spi.ConnectionFactoryOptions;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.r2dbc.R2DBCDatabaseContainer;

@RequiredArgsConstructor
public class MySQLR2DBCDatabaseContainer extends R2DBCDatabaseContainer<MySQLR2DBCDatabaseContainer> {

    @Delegate(types = Startable.class)
    private final MySQLContainer<?> container;

    public static ConnectionFactoryOptions getOptions(MySQLContainer<?> container) {
        ConnectionFactoryOptions options = ConnectionFactoryOptions
            .builder()
            .option(ConnectionFactoryOptions.DRIVER, MySQLR2DBCDatabaseContainerProvider.DRIVER)
            .build();

        return new MySQLR2DBCDatabaseContainer(container).configure(options);
    }

    @Override
    public ConnectionFactoryOptions configure(ConnectionFactoryOptions options) {
        return options
            .mutate()
            .option(ConnectionFactoryOptions.HOST, container.getHost())
            .option(ConnectionFactoryOptions.PORT, container.getMappedPort(MySQLContainer.MYSQL_PORT))
            .option(ConnectionFactoryOptions.DATABASE, container.getDatabaseName())
            .option(ConnectionFactoryOptions.USER, container.getUsername())
            .option(ConnectionFactoryOptions.PASSWORD, container.getPassword())
            .build();
    }
}
