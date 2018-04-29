package org.testcontainers.containers;

import lombok.extern.slf4j.Slf4j;

/**
 * Base class for classes that can provide a JDBC container.
 */
@Slf4j
public abstract class JdbcDatabaseContainerProvider {

    public abstract boolean supports(String databaseType);

    public JdbcDatabaseContainer newInstance() {
        log.warn("No explicit version tag was provided in JDBC URL and this class ({}) does not " +
            "override newInstance() to set a default tag. `latest` will be used but results may " +
            "be unreliable!", this.getClass().getCanonicalName());
        return this.newInstance("latest");
    }

    public abstract JdbcDatabaseContainer newInstance(String tag);
}
