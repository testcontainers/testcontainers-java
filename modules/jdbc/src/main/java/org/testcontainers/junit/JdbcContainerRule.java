package org.testcontainers.junit;

import org.testcontainers.containers.JdbcDatabaseContainer;


/**
 *
 */
public abstract class JdbcContainerRule extends GenericContainerRule {
    public JdbcContainerRule(String dockerImageName) {
        super(dockerImageName);
    }

    public JdbcContainerRule(JdbcDatabaseContainer container) {
        super(container);
    }

    protected JdbcDatabaseContainer container() {
        return (JdbcDatabaseContainer) container;
    }

    public String getJdbcUrl() {
        return container().getJdbcUrl();
    }

    public String getUsername() {
        return container().getUsername();
    }

    public String getPassword() {
        return container().getPassword();
    }
}