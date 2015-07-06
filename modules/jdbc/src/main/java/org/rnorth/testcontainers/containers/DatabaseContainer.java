package org.rnorth.testcontainers.containers;

import org.rnorth.testcontainers.containers.traits.LinkableContainer;
import org.rnorth.testcontainers.utility.Retryables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * @author richardnorth
 */
public abstract class DatabaseContainer extends AbstractContainer implements LinkableContainer {

    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseContainer.class);
    private static final Object DRIVER_MUTEX = new Object();
    private Driver driver;

    public abstract String getName();

    public abstract String getDriverClassName();

    public abstract String getJdbcUrl();

    public abstract String getUsername();

    public abstract String getPassword();

    public abstract String getTestQueryString();

    @Override
    protected void waitUntilContainerStarted() {

        Retryables.retryUntilSuccess(30, TimeUnit.SECONDS, new Retryables.UnreliableSupplier<Connection>() {
            @Override
            public Connection get() throws Exception {
                Connection connection = createConnection("");

                boolean success = connection.createStatement().execute(DatabaseContainer.this.getTestQueryString());

                if (success) {
                    LOGGER.info("Obtained a connection to container ({})", DatabaseContainer.this.getJdbcUrl());
                    return connection;
                } else {
                    throw new SQLException("Failed to execute test query");
                }
            }
        });
    }

    public Driver getJdbcDriverInstance() {

        synchronized (DRIVER_MUTEX) {
            if (driver == null) {
                try {
                    driver = (Driver) ClassLoader.getSystemClassLoader().loadClass(this.getDriverClassName()).newInstance();
                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                    throw new RuntimeException("Could not get Driver", e);
                }
            }
        }

        return driver;
    }

    public Connection createConnection(String queryString) throws SQLException {
        Properties info = new Properties();
        info.put("user", this.getUsername());
        info.put("password", this.getPassword());
        return getJdbcDriverInstance().connect(this.getJdbcUrl() + queryString, info);
    }
}
