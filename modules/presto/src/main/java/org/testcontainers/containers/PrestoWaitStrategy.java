package org.testcontainers.containers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rnorth.ducttape.unreliables.Unreliables;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

/**
 * Custom wait strategy for Presto.
 */
@RequiredArgsConstructor
@Slf4j
public final class PrestoWaitStrategy extends AbstractWaitStrategy {

    private final WaitStrategyTarget target;
    
    @Override
    public void waitUntilReady(WaitStrategyTarget target) {
        PrestoContainer<?> container = (PrestoContainer<?>) target;
        Unreliables.retryUntilSuccess(
            (int) startupTimeout.getSeconds(),
            TimeUnit.SECONDS,
            () -> {
                getRateLimiter().doWhenReady(() -> {
                    try (Connection con = container.createConnection()) {
                        con.getMetaData().getDatabaseMajorVersion();
                        con.createStatement().close();
                    } catch (SQLException e) {
                        throw new RuntimeException(e);
                    }
                });
                return true;
            }
        );
    }

    @Override
    public void waitUntilReady() {
        waitUntilReady(target);
    }
}
