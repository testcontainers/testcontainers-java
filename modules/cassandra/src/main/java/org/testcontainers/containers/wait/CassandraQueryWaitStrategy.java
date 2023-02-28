package org.testcontainers.containers.wait;

import org.awaitility.core.ConditionTimeoutException;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.delegate.CassandraDatabaseDelegate;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.delegate.DatabaseDelegate;

import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

/**
 * Waits until Cassandra returns its version
 */
public class CassandraQueryWaitStrategy extends AbstractWaitStrategy {

    private static final String SELECT_VERSION_QUERY = "SELECT release_version FROM system.local";

    private static final String TIMEOUT_ERROR = "Timed out waiting for Cassandra to be accessible for query execution";

    @Override
    protected void waitUntilReady() {
        // execute select version query until success or timeout
        try {
            await()
                .atMost(startupTimeout.getSeconds(), TimeUnit.SECONDS)
                .ignoreExceptions()
                .until(() -> {
                    getRateLimiter()
                        .doWhenReady(() -> {
                            try (DatabaseDelegate databaseDelegate = getDatabaseDelegate()) {
                                databaseDelegate.execute(SELECT_VERSION_QUERY, "", 1, false, false);
                            }
                        });
                    return true;
                });
        } catch (ConditionTimeoutException e) {
            throw new ContainerLaunchException(TIMEOUT_ERROR);
        }
    }

    private DatabaseDelegate getDatabaseDelegate() {
        return new CassandraDatabaseDelegate(waitStrategyTarget);
    }
}
