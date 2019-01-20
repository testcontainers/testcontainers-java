package org.testcontainers.r2dbc;


import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import org.testcontainers.containers.PostgreSQLContainer;

public class R2dbcPostgresContainer extends PostgreSQLContainer implements R2dbcSupport {

    @Override
    public ConnectionFactory getR2dbcConnectionFactory() {
        return new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
            .host(getContainerIpAddress())
            .database(getDatabaseName())
            .password(getPassword())
            .username(getUsername())
            .port(getFirstMappedPort())
            .build());
    }}
