package org.testcontainers.oracle;

import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.r2dbc.R2DBCDatabaseContainer;

import java.util.Set;

public class OracleR2DBCDatabaseContainer implements R2DBCDatabaseContainer {

    private final OracleContainer container;

    public OracleR2DBCDatabaseContainer(OracleContainer container) {
        this.container = container;
    }

    public static ConnectionFactoryOptions getOptions(OracleContainer container) {
        ConnectionFactoryOptions options = ConnectionFactoryOptions
            .builder()
            .option(ConnectionFactoryOptions.DRIVER, OracleR2DBCDatabaseContainerProvider.DRIVER)
            .build();

        return new OracleR2DBCDatabaseContainer(container).configure(options);
    }

    @Override
    public ConnectionFactoryOptions configure(ConnectionFactoryOptions options) {
        return options
            .mutate()
            .option(ConnectionFactoryOptions.HOST, container.getHost())
            .option(ConnectionFactoryOptions.PORT, container.getMappedPort(OracleContainer.ORACLE_PORT))
            .option(ConnectionFactoryOptions.DATABASE, container.getDatabaseName())
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
