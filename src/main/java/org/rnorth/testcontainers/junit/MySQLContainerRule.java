package org.rnorth.testcontainers.junit;

import org.junit.rules.ExternalResource;
import org.rnorth.testcontainers.containers.MySQLContainer;

/**
 * @author richardnorth
 */
public class MySQLContainerRule extends ExternalResource {

    private final MySQLContainer container;

    public MySQLContainerRule() {
        container = new MySQLContainer();
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
