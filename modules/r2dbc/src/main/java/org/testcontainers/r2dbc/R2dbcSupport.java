package org.testcontainers.r2dbc;

import io.r2dbc.spi.ConnectionFactory;

/**
 * @author humblehound
 */
public interface R2dbcSupport {
    ConnectionFactory getR2dbcConnectionFactory();
}
