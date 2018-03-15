package org.testcontainers.containers;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testcontainers.jdbc.ConnectionUrl;

/**
 * Factory for MySQL containers.
 */
public class MySQLContainerProvider extends JdbcDatabaseContainerProvider {

    private static final String USER_PARAM = "user";

    private static final String PASSWORD_PARAM = "password";


    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(MySQLContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        return new MySQLContainer(MySQLContainer.IMAGE + ":" + tag);
    }

    @Override
    public JdbcDatabaseContainer newInstance(ConnectionUrl connectionUrl) {
      Objects.requireNonNull(connectionUrl, "Connection URL cannot be null");

      final String databaseName = connectionUrl.getDatabaseName().orElse("test");
      final String user = connectionUrl.getQueryParameters().getOrDefault(USER_PARAM, "test");
      final String password = connectionUrl.getQueryParameters().getOrDefault(PASSWORD_PARAM, "test");

      return newInstance(connectionUrl.getImageTag())
               .withDatabaseName(databaseName)
               .withUsername(user)
               .withPassword(password);
    }

}
