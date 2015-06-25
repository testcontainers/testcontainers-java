package org.rnorth.testcontainers.junit;

import org.junit.rules.ExternalResource;
import org.rnorth.testcontainers.containers.OracleContainer;

/**
 * Created by gusohal on 05/05/15.
 */
public class OracleContainerRule extends ExternalResource {

    private final OracleContainer container;

    public OracleContainerRule() {
        container = new OracleContainer();
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

    public String getDriverClassName() { return container.getDriverClassName(); }
}
