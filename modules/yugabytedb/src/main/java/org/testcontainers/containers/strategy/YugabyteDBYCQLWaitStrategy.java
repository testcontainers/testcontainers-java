package org.testcontainers.containers.strategy;

import java.util.concurrent.TimeUnit;

import com.datastax.oss.driver.api.core.CqlSession;
import lombok.RequiredArgsConstructor;
import org.testcontainers.containers.YCQLSessionDelegate;
import org.testcontainers.containers.YugabyteDBYCQLContainer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;

import static org.rnorth.ducttape.unreliables.Unreliables.retryUntilSuccess;

/**
 * Custom wait strategy for YCQL API.
 *
 * <p>
 * Though we can either use HTTP or PORT based wait strategy, when we create a custom
 * keyspace/role, it gets executed asynchronously. As the wait on container.start() on a
 * specific port wouldn't fully guarantee the custom object execution. It's better to
 * check the DB status with this way with a smoke test query that uses the underlying
 * custom objects and wait for the operation to complete.
 * </p>
 *
 * @author srinivasa-vasu
 */
@RequiredArgsConstructor
public final class YugabyteDBYCQLWaitStrategy extends AbstractWaitStrategy implements YCQLSessionDelegate {

	private static final String YCQL_TEST_QUERY = "SELECT release_version FROM system.local";

	private final WaitStrategyTarget target;

	@Override
	public void waitUntilReady(WaitStrategyTarget target) {
		YugabyteDBYCQLContainer container = (YugabyteDBYCQLContainer) target;
		retryUntilSuccess((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> {
			getRateLimiter().doWhenReady(() -> {
				try (CqlSession session = builder(container)) {
					session.execute(YCQL_TEST_QUERY);
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
