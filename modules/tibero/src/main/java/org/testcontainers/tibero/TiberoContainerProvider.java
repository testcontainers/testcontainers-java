package org.testcontainers.tibero;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.JdbcDatabaseContainerProvider;
import org.testcontainers.utility.DockerImageName;

/**
 * Factory for Tibero containers.
 */
public class TiberoContainerProvider extends JdbcDatabaseContainerProvider {

    private final String LICENSE_PATH = "./libs/license.xml";

    private final String CMD_HOST_NAME = "localhost";

    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(TiberoContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance() {
        return new TiberoContainer(DockerImageName.parse(TiberoContainer.IMAGE), LICENSE_PATH, CMD_HOST_NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        return new TiberoContainer(DockerImageName.parse(TiberoContainer.IMAGE), LICENSE_PATH, CMD_HOST_NAME);
    }
}
