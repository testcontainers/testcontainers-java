package org.testcontainers.containers;

import io.r2dbc.spi.ConnectionFactoryOptions;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.r2dbc.R2DBCDatabaseContainer;

@RequiredArgsConstructor
public final class PostgreSQLR2DBCDatabaseContainer implements R2DBCDatabaseContainer {

    @Delegate(types = Startable.class)
    private final PostgreSQLContainer<?> container;

    public static ConnectionFactoryOptions getOptions(PostgreSQLContainer<?> container) {
        ConnectionFactoryOptions options = ConnectionFactoryOptions.builder()
            .option(ConnectionFactoryOptions.DRIVER, PostgreSQLR2DBCDatabaseContainerProvider.DRIVER)
            .build();

        return new PostgreSQLR2DBCDatabaseContainer(container).configure(options);
    }

    @Override
    public ConnectionFactoryOptions configure(ConnectionFactoryOptions options) {
        return options.mutate()
            .option(ConnectionFactoryOptions.HOST, container.getHost())
            .option(ConnectionFactoryOptions.PORT, container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT))
            .option(ConnectionFactoryOptions.DATABASE, container.getDatabaseName())
            .option(ConnectionFactoryOptions.USER, container.getUsername())
            .option(ConnectionFactoryOptions.PASSWORD, container.getPassword())
            .build();
    }
}
