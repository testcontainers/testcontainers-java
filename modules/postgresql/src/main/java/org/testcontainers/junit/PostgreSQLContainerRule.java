package org.testcontainers.junit;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * @author richardnorth
 */
public class PostgreSQLContainerRule extends JdbcContainerRule {

    public PostgreSQLContainerRule() {
        super(new PostgreSQLContainer());
    }

    public PostgreSQLContainerRule(String dockerImageName) {
        super(new PostgreSQLContainer(dockerImageName));
    }
}
