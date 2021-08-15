package org.testcontainers.containers;

import java.time.Duration;
import java.util.Set;

import org.testcontainers.containers.strategy.YugabyteYSQLWaitStrategy;
import org.testcontainers.utility.DockerImageName;

import static java.util.Collections.singleton;
import static org.testcontainers.containers.YugabyteContainerConstants.DEFAULT_IMAGE_NAME;
import static org.testcontainers.containers.YugabyteContainerConstants.ENTRYPOINT;
import static org.testcontainers.containers.YugabyteContainerConstants.JDBC_CONNECT_PREFIX;
import static org.testcontainers.containers.YugabyteContainerConstants.JDBC_DRIVER_CLASS;
import static org.testcontainers.containers.YugabyteContainerConstants.MASTER_DASHBOARD_PORT;
import static org.testcontainers.containers.YugabyteContainerConstants.TSERVER_DASHBOARD_PORT;
import static org.testcontainers.containers.YugabyteContainerConstants.YSQL_PORT;

/**
 * YugabyteDB YSQL (Structured Query Language) API container
 *
 * @author srinivasa-vasu
 * @see <a href="https://docs.yugabyte.com/latest/api/ysql/">YSQL API</a>
 */

public class YugabyteYSQLContainer extends JdbcDatabaseContainer<YugabyteYSQLContainer> {

	private String database = "yugabyte";

	private String username = "yugabyte";

	private String password = "yugabyte";

	/**
	 * @param imageName image name
	 */
	public YugabyteYSQLContainer(final String imageName) {
		this(DockerImageName.parse(imageName));
	}

	/**
	 * @param imageName image name
	 */
	public YugabyteYSQLContainer(final DockerImageName imageName) {
		super(imageName);
		imageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
		withExposedPorts(YSQL_PORT, MASTER_DASHBOARD_PORT, TSERVER_DASHBOARD_PORT);
		waitingFor(new YugabyteYSQLWaitStrategy(this).withStartupTimeout(Duration.ofSeconds(60)));
		withCommand(ENTRYPOINT);
	}

	@Override
	public Set<Integer> getLivenessCheckPortNumbers() {
		return singleton(getMappedPort(YSQL_PORT));
	}

	/**
	 * Configures the environment variables. Setting up these variables would create the
	 * custom objects. Setting {@link #withDatabaseName(String)},
	 * {@link #withUsername(String)}, {@link #withPassword(String)} these parameters will
	 * initilaize the database with those custom values
	 */

	@Override
	protected void configure() {
		addEnv("YSQL_DB", database);
		addEnv("YSQL_USER", username);
		addEnv("YSQL_PASSWORD", password);
	}

	@Override
	public String getDriverClassName() {
		return JDBC_DRIVER_CLASS;
	}

	@Override
	public String getJdbcUrl() {
		return JDBC_CONNECT_PREFIX + "://" + getHost() + ":" + getMappedPort(YSQL_PORT) + "/" + database
				+ constructUrlParameters("?", "&");
	}

	@Override
	public String getDatabaseName() {
		return database;
	}

	@Override
	public String getUsername() {
		return username;
	}

	@Override
	public String getPassword() {
		return password;
	}

	@Override
	public String getTestQueryString() {
		return "SELECT 1";
	}

	/**
	 * Setting this would create the keyspace
	 * @param database database name
	 * @return {@link YugabyteYSQLContainer} instance
	 */

	@Override
	public YugabyteYSQLContainer withDatabaseName(final String database) {
		this.database = database;
		return this;
	}

	/**
	 * Setting this would create the custom user role
	 * @param username user name
	 * @return {@link YugabyteYSQLContainer} instance
	 */

	@Override
	public YugabyteYSQLContainer withUsername(final String username) {
		this.username = username;
		return this;
	}

	/**
	 * Setting this along with {@link #withUsername(String)} would enable authentication
	 * @param password password
	 * @return {@link YugabyteYSQLContainer} instance
	 */

	@Override
	public YugabyteYSQLContainer withPassword(final String password) {
		this.password = password;
		return this;
	}

}
