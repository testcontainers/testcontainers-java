package org.testcontainers.containers;

import io.r2dbc.spi.ConnectionFactoryOptions;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.r2dbc.R2DBCDatabaseContainer;

@RequiredArgsConstructor
public class MSSQLR2DBCDatabaseContainer implements R2DBCDatabaseContainer {

    @Delegate(types = Startable.class)
    private final MSSQLServerContainer<?> container;

    public static ConnectionFactoryOptions getOptions(MSSQLServerContainer<?> container) {
        ConnectionFactoryOptions options = ConnectionFactoryOptions.builder()
            .option(ConnectionFactoryOptions.DRIVER, MSSQLR2DBCDatabaseContainerProvider.DRIVER)
            .build();

        return new MSSQLR2DBCDatabaseContainer(container).configure(options);
    }

    @Override
    public ConnectionFactoryOptions configure(ConnectionFactoryOptions options) {
        return options.mutate()
            .option(ConnectionFactoryOptions.HOST, container.getHost())
            .option(ConnectionFactoryOptions.PORT, container.getMappedPort(MSSQLServerContainer.MS_SQL_SERVER_PORT))
            // TODO enable if/when MSSQLServerContainer adds support for customizing the DB name
            // .option(ConnectionFactoryOptions.DATABASE, container.getDatabasseName())
            .option(ConnectionFactoryOptions.USER, container.getUsername())
            .option(ConnectionFactoryOptions.PASSWORD, container.getPassword())
            .build();
    }
}
