package org.testcontainers.containers.wait;

import org.jetbrains.annotations.NotNull;
import org.rnorth.ducttape.TimeoutException;
import org.testcontainers.containers.CassandraContainer;
import org.testcontainers.containers.ContainerLaunchException;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.delegate.CassandraDatabaseDelegate;
import org.testcontainers.delegate.DatabaseDelegate;

import java.util.concurrent.TimeUnit;

import static org.rnorth.ducttape.unreliables.Unreliables.retryUntilSuccess;

/**
 * Waits until Cassandra returns its version
 *
 * @author Eugeny Karpov
 */
public class CassandraQueryWaitStrategy extends GenericContainer.AbstractWaitStrategy {

    private static final String SELECT_VERSION_QUERY = "SELECT release_version FROM system.local";
    private static final String TIMEOUT_ERROR = "Timed out waiting for Cassandra to be accessible for query execution";

    @Override
    protected void waitUntilReady() {
        CassandraContainer cassandraContainer = getCassandraContainer();

        // execute select version query until success or timeout
        try {
            retryUntilSuccess((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> {
                getRateLimiter().doWhenReady(() -> {
                    try (DatabaseDelegate databaseDelegate = getDatabaseDelegate(cassandraContainer)) {
                        databaseDelegate.execute(SELECT_VERSION_QUERY, "", 1, false, false);
                    }
                });
                return true;
            });
        } catch (TimeoutException e) {
            throw new ContainerLaunchException(TIMEOUT_ERROR);
        }
    }

    /**
     * Cast generic container to Cassandra container or throw exception
     *
     * @throws UnsupportedOperationException if containter is null or is not Cassandra container
     */
    @NotNull
    private CassandraContainer getCassandraContainer() {
        if (container instanceof CassandraContainer) {
            return (CassandraContainer) container;
        } else {
            throw new IllegalStateException("Unsupported container type");
        }
    }

    private DatabaseDelegate getDatabaseDelegate(CassandraContainer container) {
        return new CassandraDatabaseDelegate(container);
    }
}
