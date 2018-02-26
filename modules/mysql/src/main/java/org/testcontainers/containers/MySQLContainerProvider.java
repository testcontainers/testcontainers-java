package org.testcontainers.containers;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.testcontainers.jdbc.ConnectionUrl;

/**
 * Factory for MySQL containers.
 */
public class MySQLContainerProvider extends JdbcDatabaseContainerProvider {
  
    /**
     * Groups URL of format "jdbc:tc:(databaseType):(optinal_image_tag)//(hostanme)(optional :(numeric_port))/(databasename)(?parameters)"
     */
    private static final Pattern MYSQL_URL_MATCHING_PATTERN = Pattern.compile("jdbc:tc:([a-z]+)(:([^:]+))?://([^:]+)(:([0-9]+))?/([^\\\\?]+)(\\\\?.*)?");
   
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
    public JdbcDatabaseContainer newInstance(ConnectionUrl url) {
      Objects.requireNonNull(url, "Connection URL cannot be null");
      
      Matcher urlMatcher = MYSQL_URL_MATCHING_PATTERN.matcher(url.getUrl());
      
      if(!urlMatcher.matches()) {
        //TODO: Is this necessary?
        throw new IllegalArgumentException("JDBC URL matches jdbc:tc: prefix but does not match the Expected MySQL URL Format");
      }

      final String databaseName = url.getDatabaseName().orElse("test");
      final String user = url.getQueryParameters().getOrDefault(USER_PARAM, "test");
      final String password = url.getQueryParameters().getOrDefault(PASSWORD_PARAM, "test");
      
      return newInstance(url.getImageTag())
               .withDatabaseName(databaseName)
               .withUsername(user)
               .withPassword(password);
    }

}
