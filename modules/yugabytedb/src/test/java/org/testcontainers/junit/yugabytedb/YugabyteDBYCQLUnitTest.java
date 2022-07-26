package org.testcontainers.junit.yugabytedb;

import java.net.InetSocketAddress;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import org.junit.Assert;
import org.junit.Test;
import org.testcontainers.containers.YugabyteDBYCQLContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * YugabyteDB YCQL API unit test class
 *
 * @author srinivasa-vasu
 */
public class YugabyteDBYCQLUnitTest {

	private static final String IMAGE_NAME = "yugabytedb/yugabyte:2.14.0.0-b94";

	private static final DockerImageName YBDB_TEST_IMAGE = DockerImageName.parse(IMAGE_NAME);

	private static final String LOCAL_DC = "datacenter1";

	private static final int YCQL_PORT = 9042;

	@Test
	public void testSmoke() {
		try (
            // creatingYCQLContainer {
            final YugabyteDBYCQLContainer ycqlContainer = new YugabyteDBYCQLContainer(IMAGE_NAME)
    		// }
		) {
			// startingYCQLContainer {
			ycqlContainer.start();
			// }
			Assert.assertNotNull("Smoke test simple query execution fails!",
					performQuery(ycqlContainer, "SELECT release_version FROM system.local").one().getString(0));
		}
	}

	@Test
	public void testCustomKeyspace() throws InterruptedException {
		String key = "random";
		try (final YugabyteDBYCQLContainer ycqlContainer = new YugabyteDBYCQLContainer(YBDB_TEST_IMAGE)
				.withKeyspaceName(key)) {
			ycqlContainer.start();
			Assert.assertEquals("Custom keyspace creation fails!", key,
					performQuery(ycqlContainer,
							"SELECT keyspace_name FROM system_schema.keyspaces where keyspace_name='" + key + "'").one()
									.getString(0));
		}
	}

	@Test
	public void testAuthenticationEnabled() throws InterruptedException {
		String role = "random";
		try (final YugabyteDBYCQLContainer ycqlContainer = new YugabyteDBYCQLContainer(YBDB_TEST_IMAGE)
				.withUsername(role).withPassword(role)) {
			ycqlContainer.start();
			Assert.assertEquals("Keyspace login fails with authentication enabled!", role,
					performQuery(ycqlContainer, "SELECT role FROM system_auth.roles where role='" + role + "'").one()
							.getString(0));
		}
	}

	@Test
	public void testAuthenticationDisabled() {
		try (final YugabyteDBYCQLContainer ycqlContainer = new YugabyteDBYCQLContainer(YBDB_TEST_IMAGE).withPassword("")
				.withUsername("")) {
			ycqlContainer.start();
			Assert.assertTrue("Query execution fails!",
					performQuery(ycqlContainer, "SELECT release_version FROM system.local").wasApplied());
		}
	}

	@Test
	public void testInitScript() throws InterruptedException {
		String key = "random";
		try (final YugabyteDBYCQLContainer ycqlContainer = new YugabyteDBYCQLContainer(YBDB_TEST_IMAGE)
				.withKeyspaceName(key).withUsername(key).withPassword(key).withInitScript("init/init_yql.sql")) {
			ycqlContainer.start();
			Assert.assertTrue("Query execution fails to execute statements from a custom script!",
					performQuery(ycqlContainer, "SELECT * FROM random.bar").wasApplied());
		}
	}

	private ResultSet performQuery(YugabyteDBYCQLContainer ycqlContainer, String cql) {
		try (CqlSession session = CqlSession.builder().withKeyspace(ycqlContainer.getKeyspace())
				.withAuthCredentials(ycqlContainer.getUsername(), ycqlContainer.getPassword())
				.withLocalDatacenter(LOCAL_DC)
				.addContactPoint(new InetSocketAddress(ycqlContainer.getHost(), ycqlContainer.getMappedPort(YCQL_PORT)))
				.build()) {
			return session.execute(cql);
		}
	}

}
