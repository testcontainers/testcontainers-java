package org.rnorth.testcontainers.jdbc;

import org.rnorth.testcontainers.containers.DatabaseContainer;
import org.rnorth.testcontainers.containers.MySQLContainer;
import org.rnorth.testcontainers.containers.PostgreSQLContainer;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 */
public class ContainerDatabaseDriver implements Driver {

    public static final Pattern URL_MATCHING_PATTERN = Pattern.compile("jdbc:tc:(mysql|postgresql)(:([^:]+))?://.*");
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(ContainerDatabaseDriver.class);
    static {
        load();
    }
    private Driver delegate;

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
    public Connection connect(String url, Properties info) throws SQLException {


        Matcher urlMatcher = URL_MATCHING_PATTERN.matcher(url);
        if (!urlMatcher.matches()) {
            throw new IllegalArgumentException("JDBC URL matches jdbc:tc: prefix but the database or tag name could not be identified");
        }
        String database = urlMatcher.group(1);
        String tag = urlMatcher.group(3);

        DatabaseContainer container;
        if ("mysql".equals(database)) {
            container = new MySQLContainer(tag);
            delegate = getDriver("com.mysql.jdbc.Driver");
        } else if ("postgresql".equals(database)) {
            container = new PostgreSQLContainer(tag);
            delegate = getDriver("org.postgresql.Driver");
        } else {
            throw new UnsupportedOperationException("Database name " + database + " not supported");
        }
        container.start();

        info.put("user", container.getUsername());
        info.put("password", container.getPassword());
        return delegate.connect(container.getJdbcUrl(), info);
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
