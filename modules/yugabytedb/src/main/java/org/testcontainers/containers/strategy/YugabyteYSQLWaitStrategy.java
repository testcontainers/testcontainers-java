package org.testcontainers.containers.strategy;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeUnit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.YugabyteYSQLContainer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;

import static org.rnorth.ducttape.unreliables.Unreliables.retryUntilSuccess;
import static org.testcontainers.containers.YugabyteContainerConstants.YSQL_TEST_QUERY;

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
 *
 * @author srinivasa-vasu
 */
@RequiredArgsConstructor
@Slf4j
public final class YugabyteYSQLWaitStrategy extends AbstractWaitStrategy {

	private final WaitStrategyTarget target;

	@Override
	public void waitUntilReady(WaitStrategyTarget target) {
		YugabyteYSQLContainer container = (YugabyteYSQLContainer) target;
		retryUntilSuccess((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> {
			getRateLimiter().doWhenReady(() -> {
				try (Connection con = container.createConnection(container.getJdbcUrl())) {
					con.createStatement().execute(YSQL_TEST_QUERY);
				}
				catch (SQLException ex) {
					log.error("Error connecting to the database", ex);
				}
			});
			return true;
		});
	}

	@Override
	public void waitUntilReady() {
		waitUntilReady(target);
	}

}
