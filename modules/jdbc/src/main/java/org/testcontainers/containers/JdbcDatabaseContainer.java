package org.testcontainers.containers;

import com.github.dockerjava.api.command.InspectContainerResponse;
import java.sql.Statement;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.containers.traits.LinkableContainer;
import org.testcontainers.delegate.DatabaseDelegate;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.jdbc.JdbcDatabaseDelegate;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;

import static java.util.stream.Collectors.joining;

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
    protected Map<String, String> urlParameters = new HashMap<>();

    private int startupTimeoutSeconds = 120;
    private int connectTimeoutSeconds = 120;

    private static final String QUERY_PARAM_SEPARATOR = "&";

    /**
     * @deprecated use {@link JdbcDatabaseContainer(DockerImageName)} instead
     */
    public JdbcDatabaseContainer(@NonNull final String dockerImageName) {
        this(DockerImageName.parse(dockerImageName));
    }

    public JdbcDatabaseContainer(@NonNull final Future<String> image) {
        super(image);
    }

    public JdbcDatabaseContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
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

    public SELF withUrlParam(String paramName, String paramValue) {
        urlParameters.put(paramName, paramValue);
        return self();
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

    @SneakyThrows(InterruptedException.class)
    @Override
    protected void waitUntilContainerStarted() {
        logger().info("Waiting for database connection to become available at {} using query '{}'", getJdbcUrl(), getTestQueryString());

        // Repeatedly try and open a connection to the DB and execute a test query
        long start = System.currentTimeMillis();

        while (System.currentTimeMillis() < start + (1000 * startupTimeoutSeconds)) {
            if (!isRunning()) {
                Thread.sleep(100L);
            } else {
                try (Statement statement = createConnection("").createStatement()) {
                    boolean testQuerySucceeded = statement.execute(this.getTestQueryString());
                    if (testQuerySucceeded) {
                        logger().info("Container is started (JDBC URL: {})", this.getJdbcUrl());
                        return;
                    }
                } catch (NoDriverFoundException e) {
                    // we explicitly want this exception to fail fast without retries
                    throw e;
                } catch (Exception e) {
                    // ignore so that we can try again
                    logger().debug("Failure when trying test query", e);
                    Thread.sleep(100L);
                }
            }
        }

        throw new IllegalStateException(
            String.format("Container is started, but cannot be accessed by (JDBC URL: %s), please check container logs",
                this.getJdbcUrl())
        );
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

        SQLException lastException = null;
        try {
            long start = System.currentTimeMillis();
            // give up if we hit the time limit or the container stops running for some reason
            while (System.currentTimeMillis() < start + (1000 * connectTimeoutSeconds) && isRunning()) {
                try {
                    logger().debug("Trying to create JDBC connection using {} to {} with properties: {}", driver.getClass().getName(), url, info);

                    return jdbcDriverInstance.connect(url, info);
                } catch (SQLException e) {
                    lastException = e;
                    Thread.sleep(100L);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        throw new SQLException("Could not create new connection", lastException);
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
        String baseUrl = getJdbcUrl();

        if ("".equals(queryString)) {
            return baseUrl;
        }

        if (!queryString.startsWith("?")) {
            throw new IllegalArgumentException("The '?' character must be included");
        }

        return baseUrl.contains("?")
            ? baseUrl + QUERY_PARAM_SEPARATOR + queryString.substring(1)
            : baseUrl + queryString;
    }

    protected String constructUrlParameters(String startCharacter, String delimiter) {
        return constructUrlParameters(startCharacter, delimiter, StringUtils.EMPTY);
    }

    protected String constructUrlParameters(String startCharacter, String delimiter, String endCharacter) {
        String urlParameters = "";
        if (!this.urlParameters.isEmpty()) {
            String additionalParameters = this.urlParameters.entrySet().stream()
                .map(Object::toString)
                .collect(joining(delimiter));
            urlParameters = startCharacter + additionalParameters + endCharacter;
        }
        return urlParameters;
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
