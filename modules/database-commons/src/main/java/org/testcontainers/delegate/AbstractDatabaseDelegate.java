package org.testcontainers.delegate;

import java.util.Collection;

/**
 * @param <CONNECTION> connection to the database
 * @author Eugeny Karpov
 */
public abstract class AbstractDatabaseDelegate<CONNECTION> implements DatabaseDelegate {

    /**
     * Database connection
     */
    private CONNECTION connection;

    private boolean isConnectionStarted = false;

    /**
     * Get or create new connection to the database
     */
    protected CONNECTION getConnection() {
        if (!isConnectionStarted) {
            connection = createNewConnection();
            isConnectionStarted = true;
        }
        return connection;
    }

    @Override
    public void execute(Collection<String> statements, String scriptPath, boolean continueOnError, boolean ignoreFailedDrops) {
        int lineNumber = 0;
        for (String statement : statements) {
            lineNumber++;
            execute(statement, scriptPath, lineNumber, continueOnError, ignoreFailedDrops);
        }
    }

    @Override
    public void close() {
        if (isConnectionStarted) {
            closeConnectionQuietly(connection);
            isConnectionStarted = false;
        }
    }

    /**
     * Quietly close the connection
     */
    protected abstract void closeConnectionQuietly(CONNECTION connection);

    /**
     * Template method for creating new connections to the database
     */
    protected abstract CONNECTION createNewConnection();
}
