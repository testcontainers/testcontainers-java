package org.testcontainers.postgresql;

import io.r2dbc.postgresql.PostgresqlConnectionFactoryProvider;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.lifecycle.Startable;
import org.testcontainers.r2dbc.R2DBCDatabaseContainer;

import java.util.Set;

public final class PostgreSQLR2DBCDatabaseContainer implements R2DBCDatabaseContainer {

    private final PostgreSQLContainer container;

    public PostgreSQLR2DBCDatabaseContainer(PostgreSQLContainer container) {
        this.container = container;
    }

    public static ConnectionFactoryOptions getOptions(PostgreSQLContainer container) {
        ConnectionFactoryOptions options = ConnectionFactoryOptions
            .builder()
            .option(ConnectionFactoryOptions.DRIVER, PostgresqlConnectionFactoryProvider.POSTGRESQL_DRIVER)
            .build();

        return new PostgreSQLR2DBCDatabaseContainer(container).configure(options);
    }

    /**
     * Returns the R2DBC URL for connecting to the PostgreSQL database.
     *
     * @param container the PostgreSQL container instance
     * @return the R2DBC URL in the format: r2dbc:postgresql://username:password@host:port/database
     */
    public static String getR2dbcUrl(PostgreSQLContainer container) {
        return String.format(
            "r2dbc:%s://%s:%s@%s:%d/%s",
            PostgresqlConnectionFactoryProvider.POSTGRESQL_DRIVER,
            container.getUsername(),
            container.getPassword(),
            container.getHost(),
            container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
            container.getDatabaseName()
        );
    }

    @Override
    public ConnectionFactoryOptions configure(ConnectionFactoryOptions options) {
        return options
            .mutate()
            .option(ConnectionFactoryOptions.HOST, container.getHost())
            .option(ConnectionFactoryOptions.PORT, container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT))
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

    @Override
    public String getR2dbcUrl() {
        return getR2dbcUrl(this.container);
    }
}
