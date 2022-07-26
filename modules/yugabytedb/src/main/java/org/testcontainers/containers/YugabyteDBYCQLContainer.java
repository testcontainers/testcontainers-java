package org.testcontainers.containers;

import java.net.InetSocketAddress;
import java.time.Duration;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.delegate.YugabyteDBYCQLDelegate;
import org.testcontainers.containers.strategy.YugabyteDBYCQLWaitStrategy;
import org.testcontainers.ext.ScriptUtils;
import org.testcontainers.utility.DockerImageName;

/**
 * YugabyteDB YCQL (Cloud Query Language) API container
 *
 * @author srinivasa-vasu
 * @see <a href="https://docs.yugabyte.com/latest/api/ycql/">YCQL API</a>
 */
public class YugabyteDBYCQLContainer extends GenericContainer<YugabyteDBYCQLContainer> {

	private static final DockerImageName DEFAULT_IMAGE_NAME = DockerImageName.parse("yugabytedb/yugabyte");

	private static final Integer YCQL_PORT = 9042;

	private static final Integer MASTER_DASHBOARD_PORT = 7000;

	private static final Integer TSERVER_DASHBOARD_PORT = 9000;

	private static final String ENTRYPOINT = "bin/yugabyted start --daemon=false";

	private static final String LOCAL_DC = "datacenter1";

	private String keyspace;

	private String username;

	private String password;

	private String initScript;

	/**
	 * @param imageName image name
	 */
	public YugabyteDBYCQLContainer(final String imageName) {
		this(DockerImageName.parse(imageName));
	}

	/**
	 * @param imageName image name
	 */
	public YugabyteDBYCQLContainer(final DockerImageName imageName) {
		super(imageName);
		imageName.assertCompatibleWith(DEFAULT_IMAGE_NAME);
		withExposedPorts(YCQL_PORT, MASTER_DASHBOARD_PORT, TSERVER_DASHBOARD_PORT);
		waitingFor(new YugabyteDBYCQLWaitStrategy(this).withStartupTimeout(Duration.ofSeconds(60)));
		withCommand(ENTRYPOINT);
	}

	/**
	 * Configures the environment variables. Setting up these variables would create the
	 * custom objects. Setting {@link #withKeyspaceName(String)},
	 * {@link #withUsername(String)}, {@link #withPassword(String)} these parameters will
	 * initilaize the database with those custom values
	 */
	@Override
	protected void configure() {
		addEnv("YCQL_KEYSPACE", keyspace);
		addEnv("YCQL_USER", username);
		addEnv("YCQL_PASSWORD", password);
	}

	/**
	 * @param initScript path of the initialization script file
	 * @return {@link YugabyteDBYCQLContainer} instance
	 */
	public YugabyteDBYCQLContainer withInitScript(String initScript) {
		this.initScript = initScript;
		return this;
	}

	/**
	 * Setting this would create the keyspace
	 * @param keyspace keyspace
	 * @return {@link YugabyteDBYCQLContainer} instance
	 */
	public YugabyteDBYCQLContainer withKeyspaceName(final String keyspace) {
		this.keyspace = keyspace;
		return this;
	}

	/**
	 * Setting this would create the custom user role
	 * @param username user name
	 * @return {@link YugabyteDBYCQLContainer} instance
	 */
	public YugabyteDBYCQLContainer withUsername(final String username) {
		this.username = username;
		return this;
	}

	/**
	 * Setting this along with {@link #withUsername(String)} would enable authentication
	 * @param password password
	 * @return {@link YugabyteDBYCQLContainer} instance
	 */
	public YugabyteDBYCQLContainer withPassword(final String password) {
		this.password = password;
		return this;
	}

	/**
	 * Executes the initilization script
	 * @param containerInfo containerInfo
	 */
	@Override
	protected void containerIsStarted(InspectContainerResponse containerInfo) {
		if (initScript != null) {
			ScriptUtils.runInitScript(new YugabyteDBYCQLDelegate(getSessionBuilder()), initScript);
		}
	}

	/**
	 * Builds a {@link CqlSession} instance
	 * @return {@link CqlSession} instance
	 */
	public CqlSession getSession() {
		return getSessionBuilder().build();
	}

	/**
	 * Builder method for {#com.datastax.oss.driver.api.core.CqlSession}
	 * @return {@link CqlSessionBuilder}
	 */
	public CqlSessionBuilder getSessionBuilder() {
		return CqlSession.builder().withLocalDatacenter(LOCAL_DC).withKeyspace(this.getKeyspace())
				.withAuthCredentials(this.getUsername(), this.getPassword())
				.addContactPoint(new InetSocketAddress(this.getHost(), this.getMappedPort(YCQL_PORT)));
	}

	/**
	 * Username getter method
	 * @return username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * Password getter method
	 * @return password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Keyspace getter method
	 * @return keyspace
	 */
	public String getKeyspace() {
		return keyspace;
	}

}
