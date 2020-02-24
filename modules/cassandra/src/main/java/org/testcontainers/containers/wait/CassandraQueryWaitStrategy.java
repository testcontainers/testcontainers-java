package org.testcontainers.containers.wait;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.rnorth.ducttape.TimeoutException;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.delegate.CassandraDatabaseDelegate;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.delegate.DatabaseDelegate;

import java.util.concurrent.TimeUnit;

import static org.rnorth.ducttape.unreliables.Unreliables.retryUntilSuccess;

/**
 * Waits until Cassandra returns its version
 *
 * @author Eugeny Karpov
 */
@NoArgsConstructor
public class CassandraQueryWaitStrategy extends AbstractWaitStrategy {
    private static final String SELECT_VERSION_QUERY = "SELECT release_version FROM system.local";
    private static final String TIMEOUT_ERROR = "Timed out waiting for Cassandra to be accessible for query execution";

    private String userName;
    private String password;
    private boolean authenticationEnabled = false;

    public CassandraQueryWaitStrategy(final String userName, final String password) {
        authenticationEnabled = true;
        this.userName = userName;
        this.password = password;
    }

    @Override
    protected void waitUntilReady() {
        // execute select version query until success or timeout
        try {
            retryUntilSuccess((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> {
                getRateLimiter().doWhenReady(() -> {
                    try (DatabaseDelegate databaseDelegate = getDatabaseDelegate()) {
                        databaseDelegate.execute(SELECT_VERSION_QUERY, "", 1, false, false);
                    }
                });
                return true;
            });
        } catch (TimeoutException e) {
            throw new ContainerLaunchException(TIMEOUT_ERROR);
        }
    }

    private DatabaseDelegate getDatabaseDelegate() {
        return new CassandraDatabaseDelegate(this.waitStrategyTarget, authenticationEnabled, userName, password);
    }
}
