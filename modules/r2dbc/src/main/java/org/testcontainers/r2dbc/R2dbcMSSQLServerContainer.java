package org.testcontainers.r2dbc;

import io.r2dbc.mssql.MssqlConnectionConfiguration;
import io.r2dbc.mssql.MssqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import org.testcontainers.containers.MSSQLServerContainer;

public class R2dbcMSSQLServerContainer extends MSSQLServerContainer<R2dbcMSSQLServerContainer> implements R2dbcSupport {

    @Override
    public ConnectionFactory getR2dbcConnectionFactory() {
        return new MssqlConnectionFactory(MssqlConnectionConfiguration.builder()
            .host(getContainerIpAddress())
            .password(getPassword())
            .username(getUsername())
            .port(getFirstMappedPort())
            .build());
    }
}
