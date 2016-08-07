package org.testcontainers.jdbc;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.JdbcDatabaseContainerProvider;
import org.testcontainers.jdbc.ext.ScriptUtils;

import javax.script.ScriptException;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Test Containers JDBC proxy driver. This driver will handle JDBC URLs of the form:
 * <p>
 * <code>jdbc:tc:<i>type</i>://<i>host</i>:<i>port</i>/<i>database</i>?<i>querystring</i></code>
 * <p>
 * where <i>type</i> is a supported database type (e.g. mysql, postgresql, oracle). Behind the scenes a new
 * docker container will be launched running the required database engine. New JDBC connections will be created
 * using the database's standard driver implementation, connected to the container.
 * <p>
 * If <code>TC_INITSCRIPT</code> is set in <i>querystring</i>, it will be used as the path for an init script that
 * should be run to initialize the database after the container is created. This should be a classpath resource.
 * <p>
 * Similarly <code>TC_INITFUNCTION</code> may be a method reference for a function that can initialize the database.
 * Such a function must accept a javax.sql.Connection as its only parameter.
 * An example of a valid method reference would be <code>com.myapp.SomeClass::initFunction</code>
 */
public class ContainerDatabaseDriver implements Driver {

    private static final Pattern URL_MATCHING_PATTERN = Pattern.compile("jdbc:tc:([a-z]+)(:([^:]+))?://[^\\?]+(\\?.*)?");
    private static final Pattern INITSCRIPT_MATCHING_PATTERN = Pattern.compile(".*([\\?&]?)TC_INITSCRIPT=([^\\?&]+).*");
    private static final Pattern INITFUNCTION_MATCHING_PATTERN = Pattern.compile(".*([\\?&]?)TC_INITFUNCTION=" +
            "((\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)*\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)" +
            "::" +
            "(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)" +
            ".*");

    private static final Pattern TC_PARAM_MATCHING_PATTERN = Pattern.compile("([A-Z_]+)=([^\\?&]+)");
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ContainerDatabaseDriver.class);

    private Driver delegate;
    private static final Map<String, Set<Connection>> containerConnections = new HashMap<>();
    private static final Map<String, JdbcDatabaseContainer> jdbcUrlContainerCache = new HashMap<>();
    private static final Set<String> initializedContainers = new HashSet<>();

    static {
        load();
    }

    private static void load() {
        try {
            DriverManager.registerDriver(new ContainerDatabaseDriver());
        } catch (SQLException e) {
            LOGGER.warn("Failed to register driver", e);
        }
    }

    @Override
    public boolean acceptsURL(String url) throws SQLException {
        return url.startsWith("jdbc:tc:");
    }

    @Override
    public synchronized Connection connect(String url, final Properties info) throws SQLException {

        /*
          The driver should return "null" if it realizes it is the wrong kind of driver to connect to the given URL.
         */
        if (!acceptsURL(url)) {
            return null;
        }

        synchronized (jdbcUrlContainerCache) {

            String queryString = "";
            /*
              If we already have a running container for this exact connection string, we want to connect
              to that rather than create a new container
             */
            JdbcDatabaseContainer container = jdbcUrlContainerCache.get(url);
            if (container == null) {
                /*
                  Extract from the JDBC connection URL:
                   * The database type (e.g. mysql, postgresql, ...)
                   * The docker tag, if provided.
                   * The URL query string, if provided
                 */
                Matcher urlMatcher = URL_MATCHING_PATTERN.matcher(url);
                if (!urlMatcher.matches()) {
                    throw new IllegalArgumentException("JDBC URL matches jdbc:tc: prefix but the database or tag name could not be identified");
                }
                String databaseType = urlMatcher.group(1);
                String tag = urlMatcher.group(3);
                if (tag == null) {
                    tag = "latest";
                }

                queryString = urlMatcher.group(4);
                if (queryString == null) {
                    queryString = "";
                }

                Map<String, String> parameters = getContainerParameters(url);

                /*
                  Find a matching container type using ServiceLoader.
                 */
                ServiceLoader<JdbcDatabaseContainerProvider> databaseContainers = ServiceLoader.load(JdbcDatabaseContainerProvider.class);
                for (JdbcDatabaseContainerProvider candidateContainerType : databaseContainers) {
                    if (candidateContainerType.supports(databaseType)) {
                        container = candidateContainerType.newInstance(tag);
                        delegate = container.getJdbcDriverInstance();
                    }
                }
                if (container == null) {
                    throw new UnsupportedOperationException("Database name " + databaseType + " not supported");
                }

                /*
                  Cache the container before starting to prevent race conditions when a connection
                  pool is started up
                 */
                jdbcUrlContainerCache.put(url, container);

                /*
                  Pass possible container-specific parameters
                 */
                container.setParameters(parameters);

                /*
                  Start the container
                 */
                container.start();
            }

            /*
              Create a connection using the delegated driver. The container must be ready to accept connections.
             */
            Connection connection = container.createConnection(queryString);

            /*
              If this container has not been initialized, AND
              an init script or function has been specified, use it
             */
            if (!initializedContainers.contains(container.getContainerId())) {
                runInitScriptIfRequired(url, connection);
                runInitFunctionIfRequired(url, connection);
                initializedContainers.add(container.getContainerId());
            }

            return wrapConnection(connection, container, url);
        }
    }

    private Map<String, String> getContainerParameters(String url) {

        Map<String, String> results = new HashMap<>();

        Matcher matcher = TC_PARAM_MATCHING_PATTERN.matcher(url);
        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);
            results.put(key, value);
        }

        return results;
    }

    /**
     * Wrap the connection, setting up a callback to be called when the connection is closed.
     * <p>
     * When there are no more open connections, the container itself will be stopped.
     *
     * @param connection the new connection to be wrapped
     * @param container  the container which the connection is associated with
     * @param url        the testcontainers JDBC URL for this connection
     * @return the connection, wrapped
     */
    private Connection wrapConnection(final Connection connection, final JdbcDatabaseContainer container, final String url) {
        Set<Connection> connections = containerConnections.get(container.getContainerId());

        if (connections == null) {
            connections = new HashSet<>();
            containerConnections.put(container.getContainerId(), connections);
        }

        connections.add(connection);

        final Set<Connection> finalConnections = connections;

        return new ConnectionWrapper(connection, () -> {
            finalConnections.remove(connection);
            if (finalConnections.isEmpty()) {
                container.stop();
                jdbcUrlContainerCache.remove(url);
            }
        });
    }

    /**
     * Run an init script from the classpath.
     *
     * @param url        the JDBC URL to check for init script declarations.
     * @param connection JDBC connection to apply init scripts to.
     * @throws SQLException on script or DB error
     */
    private void runInitScriptIfRequired(String url, Connection connection) throws SQLException {
        Matcher matcher = INITSCRIPT_MATCHING_PATTERN.matcher(url);
        if (matcher.matches()) {
            String initScriptPath = matcher.group(2);
            try {
                URL resource = Resources.getResource(initScriptPath);
                String sql = Resources.toString(resource, Charsets.UTF_8);
                ScriptUtils.executeSqlScript(connection, initScriptPath, sql);
            } catch (IOException | IllegalArgumentException e) {
                LOGGER.warn("Could not load classpath init script: {}", initScriptPath);
                throw new SQLException("Could not load classpath init script: " + initScriptPath, e);
            } catch (ScriptException e) {
                LOGGER.error("Error while executing init script: {}", initScriptPath, e);
                throw new SQLException("Error while executing init script: " + initScriptPath, e);
            }
        }
    }

    /**
     * Run an init function (must be a public static method on an accessible class).
     *
     * @param url        the JDBC URL to check for init function declarations.
     * @param connection JDBC connection to apply init functions to.
     * @throws SQLException on script or DB error
     */
    private void runInitFunctionIfRequired(String url, Connection connection) throws SQLException {
        Matcher matcher = INITFUNCTION_MATCHING_PATTERN.matcher(url);
        if (matcher.matches()) {
            String className = matcher.group(2);
            String methodName = matcher.group(4);

            try {
                Class<?> initFunctionClazz = Class.forName(className);
                Method method = initFunctionClazz.getMethod(methodName, Connection.class);

                method.invoke(null, connection);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
                LOGGER.error("Error while executing init function: {}::{}", className, methodName, e);
                throw new SQLException("Error while executing init function: " + className + "::" + methodName, e);
            }
        }
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
        return delegate.getPropertyInfo(url, info);
    }

    @Override
    public int getMajorVersion() {
        return delegate.getMajorVersion();
    }

    @Override
    public int getMinorVersion() {
        return delegate.getMinorVersion();
    }

    @Override
    public boolean jdbcCompliant() {
        return delegate.jdbcCompliant();
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        return delegate.getParentLogger();
    }

    /**
     * Utility method to kill ALL database containers directly from test support code. It shouldn't be necessary to use this,
     * but it is provided for convenience - e.g. for situations where many different database containers are being
     * tested and cleanup is needed to limit resource usage.
     */
    public static void killContainers() {
        synchronized (jdbcUrlContainerCache) {
            jdbcUrlContainerCache.values().forEach(JdbcDatabaseContainer::stop);
            jdbcUrlContainerCache.clear();
            containerConnections.clear();
            initializedContainers.clear();
        }

    }

    /**
     * Utility method to kill a database container directly from test support code. It shouldn't be necessary to use this,
     * but it is provided for convenience - e.g. for situations where many different database containers are being
     * tested and cleanup is needed to limit resource usage.
     * @param jdbcUrl the JDBC URL of the container which should be killed
     */
    public static void killContainer(String jdbcUrl) {
        synchronized (jdbcUrlContainerCache) {
            JdbcDatabaseContainer container = jdbcUrlContainerCache.get(jdbcUrl);
            if (container != null) {
                container.stop();
                jdbcUrlContainerCache.remove(jdbcUrl);
                containerConnections.remove(container.getContainerId());
                initializedContainers.remove(container.getContainerId());
            }
        }
    }
}
