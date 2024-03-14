package org.testcontainers.containers.strategy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.YugabyteDBYSQLContainer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.TimeUnit;

import static org.rnorth.ducttape.unreliables.Unreliables.retryUntilSuccess;

/**
 * Custom wait strategy for YSQL API.
 *
 * <p>
 * Though we can either use HTTP or PORT based wait strategy, when we create a custom
 * database/role, it gets executed asynchronously. As the wait on container.start() on a
 * specific port wouldn't fully guarantee the custom object execution. It's better to
 * check the DB status with this way with a smoke test query that uses the underlying
 * custom objects and wait for the operation to complete.
 * </p>
 */
@RequiredArgsConstructor
@Slf4j
public final class YugabyteDBYSQLWaitStrategy extends AbstractWaitStrategy {

    private final WaitStrategyTarget target;

    private static final String YSQL_EXTENDED_PROBE =
        "CREATE TABLE IF NOT EXISTS YB_SAMPLE(k int, v int, primary key(k, v))";

    private static final String YSQL_EXTENDED_PROBE_DROP_TABLE = "DROP TABLE IF EXISTS YB_SAMPLE";

    @Override
    public void waitUntilReady(WaitStrategyTarget target) {
        YugabyteDBYSQLContainer container = (YugabyteDBYSQLContainer) target;
        retryUntilSuccess(
            (int) startupTimeout.getSeconds(),
            TimeUnit.SECONDS,
            () -> {
                getRateLimiter()
                    .doWhenReady(() -> {
                        try (Connection con = container.createConnection(""); Statement stmt = con.createStatement()) {
                            stmt.execute(YSQL_EXTENDED_PROBE);
                            stmt.execute(YSQL_EXTENDED_PROBE_DROP_TABLE);
                        } catch (SQLException ex) {
                            throw new RuntimeException(ex);
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
