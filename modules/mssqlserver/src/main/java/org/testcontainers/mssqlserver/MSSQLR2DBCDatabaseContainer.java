package org.testcontainers.mssqlserver;

import io.r2dbc.mssql.MssqlConnectionFactoryProvider;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.r2dbc.R2DBCDatabaseContainer;

import java.util.Set;

public class MSSQLR2DBCDatabaseContainer implements R2DBCDatabaseContainer {

    private final MSSQLServerContainer container;

    public MSSQLR2DBCDatabaseContainer(MSSQLServerContainer container) {
        this.container = container;
    }

    public static ConnectionFactoryOptions getOptions(MSSQLServerContainer container) {
        ConnectionFactoryOptions options = ConnectionFactoryOptions
            .builder()
            .option(ConnectionFactoryOptions.DRIVER, MssqlConnectionFactoryProvider.MSSQL_DRIVER)
            .build();

        return new MSSQLR2DBCDatabaseContainer(container).configure(options);
    }

    @Override
    public ConnectionFactoryOptions configure(ConnectionFactoryOptions options) {
        return options
            .mutate()
            .option(ConnectionFactoryOptions.HOST, container.getHost())
            .option(ConnectionFactoryOptions.PORT, container.getMappedPort(MSSQLServerContainer.MS_SQL_SERVER_PORT))
            // TODO enable if/when MSSQLServerContainer adds support for customizing the DB name
            // .option(ConnectionFactoryOptions.DATABASE, container.getDatabasseName())
            .option(ConnectionFactoryOptions.USER, container.getUsername())
            .option(ConnectionFactoryOptions.PASSWORD, container.getPassword())
            .build();
    }

    @Override
    public Set<Startable> getDependencies() {
        return this.container.getDependencies();
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
    public void close() {
        this.container.close();
    }
}
