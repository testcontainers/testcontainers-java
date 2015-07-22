package org.testcontainers.containers;

/**
 * Created by rnorth on 21/07/2015.
 */
public abstract class JdbcDatabaseContainerProvider {

    public abstract boolean supports(String databaseType);

    public abstract JdbcDatabaseContainer newInstance(String tag);
}
