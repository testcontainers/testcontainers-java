package org.testcontainers.containers.delegate;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.session.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.containers.YugabyteYCQLContainer;
import org.testcontainers.delegate.AbstractDatabaseDelegate;

/**
 * Query execution delegate class for YCQL API to delegate init-scripts statements.
 *
 * @author srinivasa-vasu
 * @see YugabyteYCQLContainer
 */
@Slf4j
@RequiredArgsConstructor
public final class YugabyteYCQLDelegate extends AbstractDatabaseDelegate<Session> {

	private final CqlSessionBuilder builder;

	@Override
	protected Session createNewConnection() {
		return builder.build();
	}

	@Override
	public void execute(String statement, String scriptPath, int lineNumber, boolean continueOnError,
			boolean ignoreFailedDrops) {
		((CqlSession) getConnection()).execute(statement);
	}

	@Override
	protected void closeConnectionQuietly(Session session) {
		if (session != null) {
			session.close();
		}
	}

}
