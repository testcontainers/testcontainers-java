package org.testcontainers.junit.yugabytedb;

import java.net.InetSocketAddress;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import org.junit.Test;
import org.testcontainers.containers.YugabyteYCQLContainer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.testcontainers.YugabyteTestContainerConstants.IMAGE_NAME;
import static org.testcontainers.YugabyteTestContainerConstants.LOCAL_DC;
import static org.testcontainers.YugabyteTestContainerConstants.YBDB_TEST_IMAGE;
import static org.testcontainers.YugabyteTestContainerConstants.YCQL_PORT;

/**
 * YugabyteDB YCQL API unit test class
 *
 * @author srinivasa-vasu
 */
public class YugabyteYCQLUnitTest {

	@Test
	public void testSmoke() {
		try (YugabyteYCQLContainer container = new YugabyteYCQLContainer(IMAGE_NAME)) {
			container.start();
			assertNotNull("Smoke test simple query execution fails!",
					performQuery(container, "SELECT release_version FROM system.local").one().getString(0));
		}
	}

	@Test
	public void testCustomKeyspace() throws InterruptedException {
		String key = "random";
		try (YugabyteYCQLContainer container = new YugabyteYCQLContainer(YBDB_TEST_IMAGE).withKeyspaceName(key)) {
			container.start();
			assertEquals("Custom keyspace creation fails!", key,
					performQuery(container,
							"SELECT keyspace_name FROM system_schema.keyspaces where keyspace_name='" + key + "'").one()
									.getString(0));
		}
	}

	@Test
	public void testAuthenticationEnabled() throws InterruptedException {
		String role = "random";
		try (YugabyteYCQLContainer container = new YugabyteYCQLContainer(YBDB_TEST_IMAGE).withUsername(role)
				.withPassword(role)) {
			container.start();
			assertEquals("Keyspace login fails with authentication enabled!", role,
					performQuery(container, "SELECT role FROM system_auth.roles where role='" + role + "'").one()
							.getString(0));
		}
	}

	@Test
	public void testAuthenticationDisabled() {
		try (YugabyteYCQLContainer container = new YugabyteYCQLContainer(YBDB_TEST_IMAGE).withPassword("")
				.withUsername("")) {
			container.start();
			assertTrue("Query execution fails!",
					performQuery(container, "SELECT release_version FROM system.local").wasApplied());
		}
	}

	@Test
	public void testInitScript() throws InterruptedException {
		String key = "random";
		try (YugabyteYCQLContainer container = new YugabyteYCQLContainer(YBDB_TEST_IMAGE).withKeyspaceName(key)
				.withUsername(key).withPassword(key).withInitScript("init/init_yql.sql")) {
			container.start();
			assertTrue("Query execution fails to execute statements from a custom script!",
					performQuery(container, "SELECT * FROM random.bar").wasApplied());
		}
	}

	private ResultSet performQuery(YugabyteYCQLContainer container, String cql) {
		try (CqlSession session = CqlSession.builder().withKeyspace(container.getKeyspace())
				.withAuthCredentials(container.getUsername(), container.getPassword()).withLocalDatacenter(LOCAL_DC)
				.addContactPoint(new InetSocketAddress(container.getHost(), container.getMappedPort(YCQL_PORT)))
				.build()) {
			return session.execute(cql);
		}
	}

}
