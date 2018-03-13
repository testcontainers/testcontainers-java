package org.testcontainers.containers;

import org.testcontainers.jdbc.ConnectionUrl;

/**
 * Base class for classes that can provide a JDBC container.
 */
public abstract class JdbcDatabaseContainerProvider {

    /**
     * Tests if the specified database type is supported by this Container Provider. It should match to the base image name.
     * @param databaseType {@link String}
     * @return <code>true</code> when provider can handle this database type, else <code>false</code>.
     */
    public abstract boolean supports(String databaseType);

    /**
     * Instantiate a new {@link JdbcDatabaseContainer} with specified image tag.
     * @param tag
     * @return Instance of {@link JdbcDatabaseContainer}
     */
    public abstract JdbcDatabaseContainer newInstance(String tag);

    /**
     * Instantiate a new {@link JdbcDatabaseContainer} using information provided with {@link ConnectionUrl}.
     * @param url {@link ConnectionUrl}
     * @return Instance of {@link JdbcDatabaseContainer}
     */
    public JdbcDatabaseContainer newInstance(ConnectionUrl url) {
        return newInstance(url.getImageTag());
    }
}
