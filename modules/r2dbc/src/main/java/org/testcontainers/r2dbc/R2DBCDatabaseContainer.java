package org.testcontainers.r2dbc;

import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.containers.GenericContainer;

public abstract class R2DBCDatabaseContainer<SELF extends R2DBCDatabaseContainer<SELF>>
    extends GenericContainer<SELF> {
    public abstract ConnectionFactoryOptions configure(ConnectionFactoryOptions options);
}
