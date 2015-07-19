package org.testcontainers.junit;

import org.junit.rules.ExternalResource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * @author richardnorth
 */
public class PostgreSQLContainerRule extends ExternalResource {
    private final PostgreSQLContainer container;

    public PostgreSQLContainerRule() {
        container = new PostgreSQLContainer();
    }

    @Override
    protected void before() throws Throwable {
        container.start();
    }

    @Override
    protected void after() {
        container.stop();
    }

    public String getJdbcUrl() {
        return container.getJdbcUrl();
    }

    public String getUsername() {
        return container.getUsername();
    }

    public String getPassword() {
        return container.getPassword();
}
}
