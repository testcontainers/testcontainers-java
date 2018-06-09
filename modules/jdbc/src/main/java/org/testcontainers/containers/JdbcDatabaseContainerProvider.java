package org.testcontainers.containers;

import org.testcontainers.jdbc.ConnectionUrl;

import lombok.extern.slf4j.Slf4j;

/**
 * Base class for classes that can provide a JDBC container.
 */
@Slf4j
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
     * Instantiate a new {@link JdbcDatabaseContainer} without any specified image tag. Subclasses <i>should</i>
     * override this method if possible, to provide a default tag that is more stable than <code>latest</code>`.
     *
     * @return Instance of {@link JdbcDatabaseContainer}
     */
    public JdbcDatabaseContainer newInstance() {
        log.warn("No explicit version tag was provided in JDBC URL and this class ({}) does not " +
            "override newInstance() to set a default tag. `latest` will be used but results may " +
            "be unreliable!", this.getClass().getCanonicalName());
        return this.newInstance("latest");
    }

    /**
     * Instantiate a new {@link JdbcDatabaseContainer} using information provided with {@link ConnectionUrl}.
     * @param url {@link ConnectionUrl}
     * @return Instance of {@link JdbcDatabaseContainer}
     */
    public JdbcDatabaseContainer newInstance(ConnectionUrl url) {
        if (url.getImageTag().isPresent()) {
            return newInstance(url.getImageTag().get());
        } else {
            return newInstance();
        }
    }
}
