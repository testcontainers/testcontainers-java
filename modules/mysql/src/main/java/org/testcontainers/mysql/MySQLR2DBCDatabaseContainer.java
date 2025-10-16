package org.testcontainers.mysql;

import io.asyncer.r2dbc.mysql.MySqlConnectionFactoryProvider;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.r2dbc.R2DBCDatabaseContainer;

import java.util.Set;

public class MySQLR2DBCDatabaseContainer implements R2DBCDatabaseContainer {

    private final MySQLContainer container;

    public MySQLR2DBCDatabaseContainer(MySQLContainer container) {
        this.container = container;
    }

    public static ConnectionFactoryOptions getOptions(MySQLContainer container) {
        ConnectionFactoryOptions options = ConnectionFactoryOptions
            .builder()
            .option(ConnectionFactoryOptions.DRIVER, MySqlConnectionFactoryProvider.MYSQL_DRIVER)
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
