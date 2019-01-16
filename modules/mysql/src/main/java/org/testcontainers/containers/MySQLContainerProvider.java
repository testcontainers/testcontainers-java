package org.testcontainers.containers;

import org.testcontainers.jdbc.ConnectionUrl;

import java.util.Objects;

/**
 * Factory for MySQL containers.
 */
public class MySQLContainerProvider extends JdbcDatabaseContainerProvider {
    private static final String USER_PARAM = "user";
    private static final String PASSWORD_PARAM = "password";
    private static final int MIN_MYSQL_VERSION = 8;

    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(MySQLContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance() {
        return newInstance(MySQLContainer.DEFAULT_TAG);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        if (tag != null) {
            return new MySQLContainer(MySQLContainer.IMAGE + ":" + tag);
        } else {
            return newInstance();
        }
    }

    @Override
    public JdbcDatabaseContainer newInstance(ConnectionUrl connectionUrl) {
        Objects.requireNonNull(connectionUrl, "Connection URL cannot be null");

        final String databaseName = connectionUrl.getDatabaseName().orElse("test");
        final String user = connectionUrl.getQueryParameters().getOrDefault(USER_PARAM, "test");
        final String password = connectionUrl.getQueryParameters().getOrDefault(PASSWORD_PARAM, "test");

        final JdbcDatabaseContainer instance;
        if (connectionUrl.getImageTag().isPresent()) {
            instance = newInstance(connectionUrl.getImageTag().get());
        } else {
            instance = newInstance();
        }
        
        if (mySqlNativePasswordIsNeeded(connectionUrl)) {
            instance.withCommand("mysqld --default-authentication-plugin=mysql_native_password");
        }
        
        return instance
            .withDatabaseName(databaseName)
            .withUsername(user)
            .withPassword(password);
    }
    
    public boolean mySqlNativePasswordIsNeeded(ConnectionUrl connectionUrl) {
        String tag  = connectionUrl.getImageTag().get();
        int version = 0;
        
        if(Objects.isNull(tag) || Objects.equals(tag, "latest") || tag.isEmpty()) {
            return true;
        }
        try {
            version = Integer.parseInt(tag.split("\\.")[0]);
        } catch (NumberFormatException e) {
            return false;
        }
        if (version >= MIN_MYSQL_VERSION) {
            return true;
        }
        return false;
    }
}
