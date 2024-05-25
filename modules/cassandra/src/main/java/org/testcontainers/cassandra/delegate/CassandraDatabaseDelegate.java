package org.testcontainers.cassandra.delegate;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.testcontainers.cassandra.CassandraContainer;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.ContainerState;
import org.testcontainers.containers.ExecConfig;
import org.testcontainers.delegate.AbstractDatabaseDelegate;
import org.testcontainers.ext.ScriptUtils.ScriptStatementFailedException;

import java.io.IOException;

/**
 * Cassandra database delegate
 */
@Slf4j
@RequiredArgsConstructor
public class CassandraDatabaseDelegate extends AbstractDatabaseDelegate<Void> {

    private final ContainerState container;

    @Override
    protected Void createNewConnection() {
        // Return null here, because we run scripts using cqlsh command directly in the container.
        // So, we don't use connection object in the execute() method.
        return null;
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
            // Use cqlsh command directly inside the container to execute statements
            // See documentation here: https://cassandra.apache.org/doc/stable/cassandra/tools/cqlsh.html
            String[] cqlshCommand = new String[]{"cqlsh", "-e", statement};

            if (this.container instanceof CassandraContainer) {
                CassandraContainer<?> cassandraContainer = ((CassandraContainer<?>) this.container);
                String username = cassandraContainer.getUsername();
                String password = cassandraContainer.getPassword();
                ArrayUtils.addAll(cqlshCommand, "-u", username, "-p", password);
            }

            Container.ExecResult result = this.container.execInContainer(ExecConfig.builder()
                .command(cqlshCommand)
                .build());
            if (result.getExitCode() == 0) {
                log.debug("Statement {} was applied", statement);
            } else {
                log.error("Statement execution failed with error: \n{}", result.getStderr());
                throw new ScriptStatementFailedException(statement, lineNumber, scriptPath);
            }
        } catch (IOException | InterruptedException e) {
            throw new ScriptStatementFailedException(statement, lineNumber, scriptPath, e);
        }
    }

    @Override
    protected void closeConnectionQuietly(Void session) {
        // Nothing to do here, because we run scripts using cqlsh command directly in the container.
    }
}
