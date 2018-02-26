package org.testcontainers.containers;

import org.testcontainers.jdbc.ConnectionUrl;

/**
 * Base class for classes that can provide a JDBC container.
 */
public abstract class JdbcDatabaseContainerProvider {

    public abstract boolean supports(String databaseType);

    public abstract JdbcDatabaseContainer newInstance(String tag);
    
    /**
     * Get the new Instance with Tag and Url. Default Implementation delegates call to {@link #newInstance(tag)} method.
     * @param tag
     * @param url
     * @return
     */
    public JdbcDatabaseContainer newInstance(ConnectionUrl url) {
      return newInstance(url.getImageTag());
    }
}
