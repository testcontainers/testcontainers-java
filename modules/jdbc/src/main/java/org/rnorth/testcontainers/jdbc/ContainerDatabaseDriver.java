package org.rnorth.testcontainers.jdbc;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.spotify.docker.client.messages.Container;
import org.rnorth.testcontainers.containers.DatabaseContainer;
import org.rnorth.testcontainers.jdbc.ext.ScriptUtils;
import org.slf4j.LoggerFactory;

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
 *
 */
public class ContainerDatabaseDriver implements Driver {

    public static final Pattern URL_MATCHING_PATTERN = Pattern.compile("jdbc:tc:(mysql|postgresql|oracle)(:([^:]+))?://[^\\?]+(\\?.*)?");
    public static final Pattern INITSCRIPT_MATCHING_PATTERN = Pattern.compile(".*([\\?&]?)TC_INITSCRIPT=([^\\?&]+).*");
    public static final Pattern INITFUNCTION_MATCHING_PATTERN = Pattern.compile(".*([\\?&]?)TC_INITFUNCTION=" +
            "((\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)*\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)" +
            "::" +
            "(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*)" +
            ".*");
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ContainerDatabaseDriver.class);

    private Driver delegate;
    private Map<Container, Set<Connection>> containerConnections = new HashMap<>();
    private Map<String, DatabaseContainer> jdbcUrlContainerCache = new HashMap<>();
    private Set<DatabaseContainer> initializedContainers = new HashSet<>();

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
    public synchronized Connection connect(String url, Properties info) throws SQLException {

        String queryString = "";
        /**
         * If we already have a running container for this exact connection string, we want to connect
         * to that rather than create a new container
         */
        DatabaseContainer container = jdbcUrlContainerCache.get(url);
        if (container == null) {
            /**
             * Extract from the JDBC connection URL:
             *  * The database type (e.g. mysql, postgresql, ...)
             *  * The docker tag, if provided.
             *  * The URL query string, if provided
             */
            Matcher urlMatcher = URL_MATCHING_PATTERN.matcher(url);
            if (!urlMatcher.matches()) {
                throw new IllegalArgumentException("JDBC URL matches jdbc:tc: prefix but the database or tag name could not be identified");
            }
            String databaseType = urlMatcher.group(1);
            String tag = urlMatcher.group(3);
            queryString = urlMatcher.group(4);
            if (queryString == null) {
                queryString = "";
            }

            /**
             * Find a matching container type using ServiceLoader.
             */
            ServiceLoader<DatabaseContainer> databaseContainers = ServiceLoader.load(DatabaseContainer.class);
            for (DatabaseContainer candidateContainerType : databaseContainers) {
                if (candidateContainerType.getName().equals(databaseType)) {
                    candidateContainerType.setTag(tag);
                    delegate = getDriver(candidateContainerType.getDriverClassName());
                    container = candidateContainerType;
                }
            }
            if (container == null) {
                throw new UnsupportedOperationException("Database name " + databaseType + " not supported");
            }

            /**
             * Cache the container before starting to prevent race conditions when a connection
             * pool is started up
             */
            jdbcUrlContainerCache.put(url, container);

            /**
             * Start the container
             */
            container.start();
        }

        /**
         * Create a connection using the delegated driver
         */
        info.put("user", container.getUsername());
        info.put("password", container.getPassword());
        Connection connection = delegate.connect(container.getJdbcUrl() + queryString, info);

        /**
         * If this container has not been initialized, AND
         * an init script or function has been specified, use it
         */
        if (!initializedContainers.contains(container)) {
            runInitScriptIfRequired(url, connection);
            runInitFunctionIfRequired(url, connection);
            initializedContainers.add(container);
        }

        return wrapConnection(connection, container, url);
    }

    /**
     * Wrap the connection, setting up a callback to be called when the connection is closed.
     *
     * When there are no more open connections, the container itself will be stopped.
     *
     * @param connection    the new connection to be wrapped
     * @param container     the container which the connection is associated with
     * @param url
     * @return              the connection, wrapped
     */
    private Connection wrapConnection(final Connection connection, final DatabaseContainer container, final String url) {
        Set<Connection> connections = containerConnections.get(connection);

        if(connections == null) {
            connections = new HashSet<>();
        }

        connections.add(connection);

        final Set<Connection> finalConnections = connections;

        return new ConnectionWrapper(connection, new Runnable() {
            @Override
            public void run() {
                finalConnections.remove(connection);
                if (finalConnections.isEmpty()) {
                    container.stop();
                    jdbcUrlContainerCache.remove(url);
                }
            }
        });
    }

    /**
     * Run an init script from the classpath.
     *
     * @param url
     * @param connection
     * @throws SQLException
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
                LOGGER.warn("Could not load classpath init script", initScriptPath);
            } catch (ScriptException e) {
                LOGGER.error("Error while executing init script", e);
            }
        }
    }

    /**
     * Run an init function (must be a public static method on an accessible class).
     *
     * @param url
     * @param connection
     * @throws SQLException
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
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        }
    }

    private Driver getDriver(String driverClassName) {
        try {
            return (Driver) ClassLoader.getSystemClassLoader().loadClass(driverClassName).newInstance();
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException("Could not get Driver", e);
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
}
