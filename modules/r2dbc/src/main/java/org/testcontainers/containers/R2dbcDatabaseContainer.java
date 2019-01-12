package org.testcontainers.containers;

import io.r2dbc.client.Handle;
import io.r2dbc.client.Query;
import io.r2dbc.client.R2dbc;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import io.r2dbc.spi.Result;
import io.r2dbc.spi.Statement;
import lombok.NonNull;
import org.reactivestreams.Publisher;
import org.rnorth.ducttape.unreliables.Unreliables;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.time.Duration;
import java.time.temporal.TemporalUnit;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.r2dbc.spi.ConnectionFactory;
import reactor.core.publisher.Mono;

/**
 * Base class for containers that expose a R2DBC connection
 *
 * @author humblehound
 */
public abstract class R2dbcDatabaseContainer<SELF extends R2dbcDatabaseContainer<SELF>> extends GenericContainer<SELF> {

    private int startupTimeoutSeconds = 120;
    private int connectTimeoutSeconds = 120;

    public R2dbcDatabaseContainer(@NonNull final String dockerImageName) {
        super(dockerImageName);
    }

    public R2dbcDatabaseContainer(@NonNull final Future<String> image) {
        super(image);
    }

    /**
     * @return the name of the actual JDBC driver to use
     */
    public abstract String getDriverClassName();

    /**
     * @return the host of the dockerized DB
     */
    public abstract String getHost();

    /**
     * @return the port of the containerized DB
     */
    protected abstract String getPort();

    /**
     * @return the database name
     */
    public abstract String getDatabaseName();

    /**
     * @return the standard database username that should be used for connections
     */
    public abstract String getUsername();

    /**
     * @return the standard password that should be used for connections
     */
    public abstract String getPassword();

    /**
     * @return a test query string suitable for testing that this particular database type is alive
     */
    protected abstract String getTestQueryString();

    @Override
    protected void waitUntilContainerStarted() {
        // Repeatedly try and open a connection to the DB and execute a test query

        logger().info("Waiting for database connection to become available at {} using query '{}'", getHost(), getTestQueryString());
        Unreliables.retryUntilSuccess(startupTimeoutSeconds, TimeUnit.SECONDS, () -> {

            if (!isRunning()) {
                throw new ContainerLaunchException("Container failed to start");
            }

            R2dbc r2dbc = new R2dbc(createConnection(""));

            int success = r2dbc.inTransaction(handle ->
                handle.createQuery(this.getTestQueryString()).mapResult(Result::getRowsUpdated)
            ).blockFirst();

            if (success > 0) {
                logger().info("Obtained a connection to container ({})", this.getTestQueryString());
                return null;
            } else {
                throw new SQLException("Failed to execute test query");
            }

        });
    }

    /**
     * Creates a connection to the underlying containerized database instance.
     *
     * @param queryString query string parameters that should be appended to the JDBC connection URL.
     *                    The '?' character must be included
     * @return a Connection
     * @throws SQLException if there is a repeated failure to create the connection
     */
    public ConnectionFactory createConnection(String queryString) throws SQLException {
        return new ConnectionFactory() {
            @Override
            public Publisher<? extends io.r2dbc.spi.Connection> create() {
                return null;
            }

            @Override
            public ConnectionFactoryMetadata getMetadata() {
                return null;
            }
        };
//        final Properties info = new Properties();
//        info.put("user", this.getUsername());
//        info.put("password", this.getPassword());
//        final String url = constructUrlForConnection(queryString);
//
//        final Driver jdbcDriverInstance = getJdbcDriverInstance();
//
//        try {
//            return Unreliables.retryUntilSuccess(getConnectTimeoutSeconds(), TimeUnit.SECONDS, () ->
//                DB_CONNECT_RATE_LIMIT.getWhenReady(() ->
//                    jdbcDriverInstance.connect(url, info)));
//        } catch (Exception e) {
//            throw new SQLException("Could not create new connection", e);
//        }
    }
}
