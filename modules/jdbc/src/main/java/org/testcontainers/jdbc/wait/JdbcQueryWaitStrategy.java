package org.testcontainers.jdbc.wait;

import org.rnorth.ducttape.TimeoutException;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.delegate.DatabaseDelegate;

import java.util.concurrent.TimeUnit;

import static org.rnorth.ducttape.unreliables.Unreliables.retryUntilSuccess;

/**
 * Waits until database returns its version
 *
 * @author Eddú
 */
public class JdbcQueryWaitStrategy extends AbstractWaitStrategy {

    private static final String SELECT_VERSION_QUERY = "SELECT 1";
    private static final String TIMEOUT_ERROR = "Timed out waiting for database to be accessible for query execution";

    private final DatabaseDelegate databaseDelegate;

    public JdbcQueryWaitStrategy(DatabaseDelegate databaseDelegate) {
        this.databaseDelegate = databaseDelegate;
    }

    @Override
    protected void waitUntilReady() {
        try {
            retryUntilSuccess((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> {
                getRateLimiter().doWhenReady(() -> {
                    try (DatabaseDelegate databaseDelegate = this.databaseDelegate) {
                        databaseDelegate.execute(SELECT_VERSION_QUERY, "", 1, false, false);
                    }
                });
                return true;
            });
        } catch (TimeoutException e) {
            throw new ContainerLaunchException(TIMEOUT_ERROR);
        }
    }

}
