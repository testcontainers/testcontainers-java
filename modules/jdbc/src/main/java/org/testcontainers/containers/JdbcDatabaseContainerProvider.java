package org.testcontainers.containers;

/**
 * Base class for classes that can provide a JDBC container.
 */
public abstract class JdbcDatabaseContainerProvider {

    public abstract boolean supports(String databaseType);

    public abstract JdbcDatabaseContainer newInstance(String tag);
}
