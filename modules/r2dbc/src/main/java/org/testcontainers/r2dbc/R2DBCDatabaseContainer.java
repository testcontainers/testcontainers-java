package org.testcontainers.r2dbc;

import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.lifecycle.Startable;

public interface R2DBCDatabaseContainer extends Startable {
    ConnectionFactoryOptions configure(ConnectionFactoryOptions options);

    /**
     * Returns the R2DBC URL for connecting to the database.
     * <p>
     * The default implementation throws {@link UnsupportedOperationException}.
     * Implementations should override this method to provide the actual R2DBC URL.
     *
     * @return the R2DBC URL in the format: r2dbc:driver://username:password@host:port/database
     * @throws UnsupportedOperationException if the implementation does not support R2DBC URLs
     */
    default String getR2dbcUrl() {
        throw new UnsupportedOperationException("R2DBC URL is not supported by this container");
    }
}
