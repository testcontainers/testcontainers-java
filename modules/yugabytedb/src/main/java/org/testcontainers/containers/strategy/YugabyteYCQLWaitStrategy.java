package org.testcontainers.containers.strategy;

import java.util.concurrent.TimeUnit;

import com.datastax.oss.driver.api.core.CqlSession;
import lombok.RequiredArgsConstructor;
import org.testcontainers.containers.YugabyteYCQLContainer;
import org.testcontainers.containers.wait.strategy.AbstractWaitStrategy;
import org.testcontainers.containers.wait.strategy.WaitStrategyTarget;

import static org.rnorth.ducttape.unreliables.Unreliables.retryUntilSuccess;
import static org.testcontainers.containers.YugabyteContainerConstants.YCQL_TEST_QUERY;

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
public final class YugabyteYCQLWaitStrategy extends AbstractWaitStrategy {

	private final WaitStrategyTarget target;

	@Override
	public void waitUntilReady(WaitStrategyTarget target) {
		YugabyteYCQLContainer container = (YugabyteYCQLContainer) target;
		retryUntilSuccess((int) startupTimeout.getSeconds(), TimeUnit.SECONDS, () -> {
			getRateLimiter().doWhenReady(() -> {
				try (CqlSession session = container.getSession()) {
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
