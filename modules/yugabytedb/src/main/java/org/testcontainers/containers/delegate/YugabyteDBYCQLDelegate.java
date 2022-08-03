package org.testcontainers.containers.delegate;

import com.datastax.oss.driver.api.core.CqlSession;
import lombok.RequiredArgsConstructor;
import org.testcontainers.containers.YCQLSessionDelegate;
import org.testcontainers.containers.YugabyteDBYCQLContainer;
import org.testcontainers.delegate.AbstractDatabaseDelegate;

/**
 * Query execution delegate class for YCQL API to delegate init-scripts statements.
 *
 * @author srinivasa-vasu
 * @see YugabyteDBYCQLContainer
 */
@RequiredArgsConstructor
public final class YugabyteDBYCQLDelegate extends AbstractDatabaseDelegate<CqlSession> implements YCQLSessionDelegate {

	private final YugabyteDBYCQLContainer container;

	@Override
	protected CqlSession createNewConnection() {
		return builder(container);
	}

	@Override
	public void execute(String statement, String scriptPath, int lineNumber, boolean continueOnError,
			boolean ignoreFailedDrops) {
		getConnection().execute(statement);
	}

	@Override
	protected void closeConnectionQuietly(CqlSession session) {
		if (session != null) {
			session.close();
		}
	}

}
