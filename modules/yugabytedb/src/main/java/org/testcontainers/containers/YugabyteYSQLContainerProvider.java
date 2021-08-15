package org.testcontainers.containers;

import org.testcontainers.jdbc.ConnectionUrl;

import static org.testcontainers.containers.YugabyteContainerConstants.DEFAULT_IMAGE_NAME;
import static org.testcontainers.containers.YugabyteContainerConstants.DEFAULT_TAG;
import static org.testcontainers.containers.YugabyteContainerConstants.NAME;
import static org.testcontainers.containers.YugabyteContainerConstants.PASSWORD_PARAM;
import static org.testcontainers.containers.YugabyteContainerConstants.USER_PARAM;

/**
 * YugabyteDB YSQL (Structured Query Language) JDBC container provider
 *
 * @author srinivasa-vasu
 */
public class YugabyteYSQLContainerProvider extends JdbcDatabaseContainerProvider {

	@Override
	public boolean supports(String databaseType) {
		return databaseType.equals(NAME);
	}

	@Override
	public JdbcDatabaseContainer newInstance() {
		return newInstance(DEFAULT_TAG);
	}

	@Override
	public JdbcDatabaseContainer newInstance(String tag) {
		return new YugabyteYSQLContainer(DEFAULT_IMAGE_NAME.withTag(tag));
	}

	@Override
	public JdbcDatabaseContainer newInstance(ConnectionUrl connectionUrl) {
		return newInstanceFromConnectionUrl(connectionUrl, USER_PARAM, PASSWORD_PARAM);
	}

}
