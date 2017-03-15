package org.testcontainers.containers;

import org.jetbrains.annotations.NotNull;
import org.rnorth.ducttape.ratelimits.RateLimiter;
import org.rnorth.ducttape.ratelimits.RateLimiterBuilder;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.traits.LinkableContainer;
import org.testcontainers.utility.MountableFile;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Base class for containers that expose a JDBC connection
 *
 * @author richardnorth
 */
public abstract class JdbcDatabaseContainer<SELF extends JdbcDatabaseContainer<SELF>> extends GenericContainer<SELF> implements LinkableContainer {

    private static final Object DRIVER_LOAD_MUTEX = new Object();
    private Driver driver;
    protected Map<String, String> parameters = new HashMap<>();

    private static final RateLimiter DB_CONNECT_RATE_LIMIT = RateLimiterBuilder.newBuilder()
            .withRate(10, TimeUnit.SECONDS)
            .withConstantThroughput()
            .build();

    public JdbcDatabaseContainer(String dockerImageName) {
        super(dockerImageName);
    }

    /**
     * @return the name of the actual JDBC driver to use
     */
    protected abstract String getDriverClassName();

    /**
     * @return a JDBC URL that may be used to connect to the dockerized DB
     */
    public abstract String getJdbcUrl();

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

        logger().info("Waiting for database connection to become available at {} using query '{}'", getJdbcUrl(), getTestQueryString());
        Unreliables.retryUntilSuccess(120, TimeUnit.SECONDS, () -> {

            if (!isRunning()) {
                throw new ContainerLaunchException("Container failed to start");
            }

            Connection connection = DB_CONNECT_RATE_LIMIT.getWhenReady(() -> createConnection(""));

            boolean success = connection.createStatement().execute(JdbcDatabaseContainer.this.getTestQueryString());

            if (success) {
                logger().info("Obtained a connection to container ({})", JdbcDatabaseContainer.this.getJdbcUrl());
                return connection;
            } else {
                throw new SQLException("Failed to execute test query");
            }
        });
    }

    /**
     * Obtain an instance of the correct JDBC driver for this particular database container type
     * @return a JDBC Driver
     */
    public Driver getJdbcDriverInstance() {

        synchronized (DRIVER_LOAD_MUTEX) {
            if (driver == null) {
                try {
                    driver = (Driver) Class.forName(this.getDriverClassName()).newInstance();
                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                    throw new RuntimeException("Could not get Driver", e);
                }
            }
        }

        return driver;
    }

    /**
     * Creates a connection to the underlying containerized database instance.
     *
     * @param queryString   any special query string parameters that should be appended to the JDBC connection URL. The
     *                      '?' character must be included
     * @return              a Connection
     * @throws SQLException if there is a repeated failure to create the connection
     */
    public Connection createConnection(String queryString) throws SQLException {
        final Properties info = new Properties();
        info.put("user", this.getUsername());
        info.put("password", this.getPassword());
        final String url = this.getJdbcUrl() + queryString;

        final Driver jdbcDriverInstance = getJdbcDriverInstance();

        try {
            return Unreliables.retryUntilSuccess(120, TimeUnit.SECONDS, () -> jdbcDriverInstance.connect(url, info));
        } catch (Exception e) {
            throw new SQLException("Could not create new connection", e);
        }
    }

    protected void optionallyMapResourceParameterAsVolume(@NotNull String paramName, @NotNull String pathNameInContainer, @NotNull String defaultResource) {
        String resourceName = parameters.getOrDefault(paramName, defaultResource);

        if (resourceName != null) {
            final MountableFile mountableFile = MountableFile.forClasspathResource(resourceName);
            addFileSystemBind(mountableFile.getResolvedPath(), pathNameInContainer, BindMode.READ_ONLY);
        }
    }

    @Override
    protected abstract Integer getLivenessCheckPort();

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @SuppressWarnings("unused")
    public void addParameter(String paramName, String value) {
        this.parameters.put(paramName, value);
    }
}
