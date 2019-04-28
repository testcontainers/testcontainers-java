package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.traits.LinkableContainer;
import org.testcontainers.delegate.DatabaseDelegate;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.jdbc.JdbcDatabaseDelegate;
import org.testcontainers.utility.MountableFile;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;

/**
 * Base class for containers that expose a JDBC connection
 *
 * @author richardnorth
 */
public abstract class JdbcDatabaseContainer<SELF extends JdbcDatabaseContainer<SELF>> extends GenericContainer<SELF> implements LinkableContainer {

    private static final Object DRIVER_LOAD_MUTEX = new Object();
    private Driver driver;
    private String initScriptPath;
    protected Map<String, String> parameters = new HashMap<>();

    private int startupTimeoutSeconds = 120;
    private int connectTimeoutSeconds = 120;

    public JdbcDatabaseContainer(@NonNull final String dockerImageName) {
        super(dockerImageName);
    }

    public JdbcDatabaseContainer(@NonNull final Future<String> image) {
        super(image);
    }

    /**
     * @return the name of the actual JDBC driver to use
     */
    public abstract String getDriverClassName();

    /**
     * @return a JDBC URL that may be used to connect to the dockerized DB
     */
    public abstract String getJdbcUrl();

    /**
     * @return the database name
     */
    public String getDatabaseName() {
        throw new UnsupportedOperationException();
    }

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

    public SELF withUsername(String username) {
        throw new UnsupportedOperationException();
    }

    public SELF withPassword(String password) {
        throw new UnsupportedOperationException();
    }

    public SELF withDatabaseName(String dbName) {
        throw new UnsupportedOperationException();

    }

    /**
     * Set startup time to allow, including image pull time, in seconds.
     *
     * @param startupTimeoutSeconds startup time to allow, including image pull time, in seconds
     * @return self
     */
    public SELF withStartupTimeoutSeconds(int startupTimeoutSeconds) {
        this.startupTimeoutSeconds = startupTimeoutSeconds;
        return self();
    }

    /**
     * Set time to allow for the database to start and establish an initial connection, in seconds.
     *
     * @param connectTimeoutSeconds time to allow for the database to start and establish an initial connection in seconds
     * @return self
     */
    public SELF withConnectTimeoutSeconds(int connectTimeoutSeconds) {
        this.connectTimeoutSeconds = connectTimeoutSeconds;
        return self();
    }

    public SELF withInitScript(String initScriptPath) {
        this.initScriptPath = initScriptPath;
        return self();
    }

    @Override
    protected void waitUntilContainerStarted() {
        // Repeatedly try and open a connection to the DB and execute a test query

        logger().info("Waiting for database connection to become available at {} using query '{}'", getJdbcUrl(), getTestQueryString());

        await().ignoreExceptionsMatching(e -> ! (e instanceof NoDriverFoundException))
                .timeout(startupTimeoutSeconds, SECONDS)
                .until(() -> {
                    if (!isRunning()) {
                        return false; // Don't attempt to connect
                    }

                    try (Connection connection = createConnection("")) {
                        boolean success = connection.createStatement().execute(JdbcDatabaseContainer.this.getTestQueryString());

                        if (success) {
                            logger().info("Obtained a connection to container ({})", JdbcDatabaseContainer.this.getJdbcUrl());
                            return true;
                        } else {
                            return false;
                        }
                    }
                });
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        runInitScriptIfRequired();
    }

    /**
     * Obtain an instance of the correct JDBC driver for this particular database container type
     *
     * @return a JDBC Driver
     */
    public Driver getJdbcDriverInstance() throws NoDriverFoundException {

        synchronized (DRIVER_LOAD_MUTEX) {
            if (driver == null) {
                try {
                    driver = (Driver) Class.forName(this.getDriverClassName()).newInstance();
                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                    throw new NoDriverFoundException("Could not get Driver", e);
                }
            }
        }

        return driver;
    }

    /**
     * Creates a connection to the underlying containerized database instance.
     *
     * @param queryString query string parameters that should be appended to the JDBC connection URL.
     *                    The '?' character must be included
     * @return a Connection
     * @throws SQLException if there is a repeated failure to create the connection
     */
    public Connection createConnection(String queryString) throws SQLException, NoDriverFoundException {
        final Properties info = new Properties();
        info.put("user", this.getUsername());
        info.put("password", this.getPassword());
        final String url = constructUrlForConnection(queryString);

        final Driver jdbcDriverInstance = getJdbcDriverInstance();

        try {
            return await()
                .ignoreExceptions()
                .atMost(connectTimeoutSeconds, SECONDS)
                .pollDelay(0, SECONDS)
                .pollInterval(5, SECONDS)
                .until(() -> jdbcDriverInstance.connect(url, info), __ -> true);
        } catch (Exception e) {
            throw new SQLException("Could not create new connection", e);
        }
    }

    /**
     * Template method for constructing the JDBC URL to be used for creating {@link Connection}s.
     * This should be overridden if the JDBC URL and query string concatenation or URL string
     * construction needs to be different to normal.
     *
     * @param queryString query string parameters that should be appended to the JDBC connection URL.
     *                    The '?' character must be included
     * @return a full JDBC URL including queryString
     */
    protected String constructUrlForConnection(String queryString) {
        return getJdbcUrl() + queryString;
    }

    protected void optionallyMapResourceParameterAsVolume(@NotNull String paramName, @NotNull String pathNameInContainer, @NotNull String defaultResource) {
        String resourceName = parameters.getOrDefault(paramName, defaultResource);

        if (resourceName != null) {
            final MountableFile mountableFile = MountableFile.forClasspathResource(resourceName);
            withCopyFileToContainer(mountableFile, pathNameInContainer);
        }
    }

    /**
     * Load init script content and apply it to the database if initScriptPath is set
     */
    protected void runInitScriptIfRequired() {
        if (initScriptPath != null) {
            ScriptUtils.runInitScript(getDatabaseDelegate(), initScriptPath);
        }
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    @SuppressWarnings("unused")
    public void addParameter(String paramName, String value) {
        this.parameters.put(paramName, value);
    }

    /**
     * @return startup time to allow, including image pull time, in seconds
     * @deprecated should not be overridden anymore, use {@link #withStartupTimeoutSeconds(int)} in constructor instead
     */
    @Deprecated
    protected int getStartupTimeoutSeconds() {
        return startupTimeoutSeconds;
    }

    /**
     * @return time to allow for the database to start and establish an initial connection, in seconds
     * @deprecated should not be overridden anymore, use {@link #withConnectTimeoutSeconds(int)} in constructor instead
     */
    @Deprecated
    protected int getConnectTimeoutSeconds() {
        return connectTimeoutSeconds;
    }

    protected DatabaseDelegate getDatabaseDelegate() {
        return new JdbcDatabaseDelegate(this, "");
    }

    public static class NoDriverFoundException extends RuntimeException {
        public NoDriverFoundException(String message, Throwable e) {
            super(message, e);
        }
    }
}
