package org.testcontainers.delegate;

import java.util.Collection;

/**
 * @param <CONTAINER>  testcontainers container
 * @param <CONNECTION> connection to the database
 * @author Eugeny Karpov
 */
public abstract class AbstractDatabaseDelegate<CONTAINER, CONNECTION> implements DatabaseDelegate {

    /**
     * Testcontainers container
     */
    protected CONTAINER container;

    /**
     * Database connection
     */
    private CONNECTION connection;

    public AbstractDatabaseDelegate(CONTAINER container) {
        this.container = container;
    }

    /**
     * Get or create new connection to the database
     */
    protected CONNECTION getConnection() {
        if (connection == null) {
            connection = createNewConnection();
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
        if (connection != null) {
            closeConnectionQuietly(connection);
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
