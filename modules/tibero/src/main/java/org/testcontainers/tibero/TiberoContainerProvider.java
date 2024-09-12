package org.testcontainers.tibero;

import static org.testcontainers.tibero.TiberoContainer.LICENSE_PATH;

import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.JdbcDatabaseContainerProvider;
import org.testcontainers.utility.DockerImageName;

/**
 * Factory for Tibero containers.
 */
public class TiberoContainerProvider extends JdbcDatabaseContainerProvider {

    @Override
    public boolean supports(String databaseType) {
        return databaseType.equals(TiberoContainer.NAME);
    }

    @Override
    public JdbcDatabaseContainer newInstance() {
        return new TiberoContainer(DockerImageName.parse(TiberoContainer.IMAGE), LICENSE_PATH);
    }

    @Override
    public JdbcDatabaseContainer newInstance(String tag) {
        return new TiberoContainer(DockerImageName.parse(TiberoContainer.IMAGE), LICENSE_PATH);
    }
}
