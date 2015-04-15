import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

/**
 *
 */
public class ContainerDatabaseDriver implements Driver {

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
        return url.startsWith("jdbc:tpc:");
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {

        if (url.startsWith("jdbc:tpc:mysql:")) {

            MySQLContainerRule rule = new MySQLContainerRule();
            info.put("user", rule.getUsername());
            info.put("password", rule.getPassword());

            try {
                rule.before();

                delegate = (Driver) ClassLoader.getSystemClassLoader().loadClass("com.mysql.jdbc.Driver").newInstance();
                return delegate.connect(rule.getJdbcUrl(), info);

            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                e.printStackTrace();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }

        return null;
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
