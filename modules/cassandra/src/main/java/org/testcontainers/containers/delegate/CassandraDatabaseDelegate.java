package org.testcontainers.containers.delegate;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.DriverException;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.delegate.AbstractDatabaseDelegate;
import org.testcontainers.exception.ConnectionCreationException;
import org.testcontainers.ext.ScriptUtils.ScriptStatementFailedException;

/**
 * Cassandra database delegate
 */
@Slf4j
@RequiredArgsConstructor
public class CassandraDatabaseDelegate extends AbstractDatabaseDelegate<CqlSession> {

    private final ContainerState container;

    @Override
    protected CqlSession createNewConnection() {
        try {
            return CassandraContainer.getCqlSession(container);
        } catch (DriverException e) {
            log.error("Could not obtain cassandra connection");
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
            if (result.wasApplied()) {
                log.debug("Statement {} was applied", statement);
            } else {
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
            log.error("Could not close cassandra connection", e);
        }
    }
}
