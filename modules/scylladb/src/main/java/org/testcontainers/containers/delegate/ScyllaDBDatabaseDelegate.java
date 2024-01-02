package org.testcontainers.containers.delegate;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DriverException;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import org.slf4j.Logger;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.ScyllaDBContainer;
import org.testcontainers.delegate.AbstractDatabaseDelegate;
import org.testcontainers.exception.ConnectionCreationException;
import org.testcontainers.ext.ScriptUtils.ScriptStatementFailedException;
import org.testcontainers.utility.DockerLoggerFactory;

import java.net.InetSocketAddress;

public class ScyllaDBDatabaseDelegate extends AbstractDatabaseDelegate<CqlSession> {

    public ScyllaDBDatabaseDelegate(ContainerState container) {
        this.container = container;
    }

    protected Logger logger() {
        return DockerLoggerFactory.getLogger(container.getCurrentContainerInfo().getName());
    }

    private final ContainerState container;

    @Override
    protected CqlSession createNewConnection() {
        try {
            return CqlSession
                .builder()
                .addContactPoint(
                    new InetSocketAddress(container.getHost(), container.getMappedPort(ScyllaDBContainer.CQL_PORT))
                )
                .withLocalDatacenter("datacenter1")
                .build();
        } catch (DriverException e) {
            throw new ConnectionCreationException("Could not obtain cassandra connection", e);
        }
    }

    @Override
    public void execute(
        String statement,
        String scriptPath,
        int lineNumber,
        boolean continueOnError,
        boolean ignoreFailedDrops
    ) {
        try {
            ResultSet result = getConnection().execute(statement);
            if (!result.wasApplied()) {
                throw new ScriptStatementFailedException(statement, lineNumber, scriptPath);
            }
        } catch (DriverException e) {
            throw new ScriptStatementFailedException(statement, lineNumber, scriptPath, e);
        }
    }

    @Override
    protected void closeConnectionQuietly(CqlSession session) {
        try {
            session.close();
        } catch (Exception e) {
            logger().error("Could not close cassandra connection", e);
        }
    }
}
