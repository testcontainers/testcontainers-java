package org.testcontainers.r2dbc;

import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.lifecycle.Startable;

public interface R2DBCDatabaseContainer extends Startable {

    ConnectionFactoryOptions configure(ConnectionFactoryOptions options);
}
