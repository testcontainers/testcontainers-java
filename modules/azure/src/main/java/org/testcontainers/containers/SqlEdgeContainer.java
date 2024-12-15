package org.testcontainers.containers;

import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

/**
 * Testcontainers implementation for Sql Edge.
 * <p>
 * Supported image: {@code mcr.microsoft.com/azure-sql-edge}
 * <p>
 * Exposed ports: 1433
 */
public class SqlEdgeContainer extends GenericContainer<SqlEdgeContainer> {
    private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse(
        "mcr.microsoft.com/azure-sql-edge"
    );

    private static final int PORT = 1433;

    /**
     * @param dockerImageName specified docker image name to run
     */
    public SqlEdgeContainer(final DockerImageName dockerImageName) {
        super(dockerImageName);
        dockerImageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
        withExposedPorts(PORT);
        waitingFor(Wait.forLogMessage(".*Service Broker manager has started.\\r\\n$", 1));
        addEnv("ACCEPT_EULA", "Y");
        addEnv("MSSQL_SA_PASSWORD", "J3R4uUWLTjDqTXoQnvXu");
    }

}
