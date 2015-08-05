package org.testcontainers.junit;

import org.testcontainers.containers.MySQLContainer;

/**
 * @author richardnorth
 */
public class MySQLContainerRule extends JdbcContainerRule {

    public MySQLContainerRule() {
        super(new MySQLContainer());
    }

    public MySQLContainerRule(String dockerImageName) {
        super(new MySQLContainer(dockerImageName));
    }

    public MySQLContainerRule withConfigurationOverride(String classpathDirectory) {
        container().addParameter(MySQLContainer.MY_CNF_CONFIG_OVERRIDE_PARAM_NAME, classpathDirectory);
        return this;
    }
}
