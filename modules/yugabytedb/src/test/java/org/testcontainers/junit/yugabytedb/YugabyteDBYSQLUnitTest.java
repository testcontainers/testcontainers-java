package org.testcontainers.junit.yugabytedb;

import java.sql.SQLException;

import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;
import org.testcontainers.containers.YugabyteDBYSQLContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.utility.DockerImageName;

/**
 * YugabyteDB YSQL API unit test class
 *
 * @author srinivasa-vasu
 */
public class YugabyteDBYSQLUnitTest extends AbstractContainerDatabaseTest {

	private static final String IMAGE_NAME = "yugabytedb/yugabyte:2.14.0.0-b94";

	private static final DockerImageName YBDB_TEST_IMAGE = DockerImageName.parse(IMAGE_NAME);

	@Test
	public void testSmoke() throws SQLException {
		try (
				// creatingYSQLContainer {
				final YugabyteDBYSQLContainer ysqlContainer = new YugabyteDBYSQLContainer(IMAGE_NAME)
		// }
		) {
			// startingYSQLContainer {
			ysqlContainer.start();
			// }
			Assert.assertEquals("Query execution fails!", 1, performQuery(ysqlContainer, "SELECT 1").getInt(1));
		}
	}

	@Test
	public void testCustomDatabase() throws SQLException {
		String key = "random";
		try (final YugabyteDBYSQLContainer ysqlContainer = new YugabyteDBYSQLContainer(YBDB_TEST_IMAGE)
				.withDatabaseName(key)) {
			ysqlContainer.start();
			Assert.assertEquals("Query execution on a custom database fails!", 1,
					performQuery(ysqlContainer, "SELECT 1").getInt(1));
		}
	}

	@Test
	public void testExplicitInitScript() throws SQLException {
		try (final YugabyteDBYSQLContainer ysqlContainer = new YugabyteDBYSQLContainer(YBDB_TEST_IMAGE)
				.withInitScript("init/init_yql.sql")) {
			ysqlContainer.start();
			Assert.assertEquals("Value from the init script does not match the real value", "hello world",
					performQuery(ysqlContainer, "SELECT foo FROM bar").getString(1));
		}
	}

	@Test
	public void testWithAdditionalUrlParamInJdbcUrl() {
		try (final YugabyteDBYSQLContainer ysqlContainer = new YugabyteDBYSQLContainer(YBDB_TEST_IMAGE)
				.withUrlParam("sslmode", "disable").withUrlParam("application_name", "yugabyte")) {
			ysqlContainer.start();
			String jdbcUrl = ysqlContainer.getJdbcUrl();
			MatcherAssert.assertThat(jdbcUrl, CoreMatchers.containsString("?"));
			MatcherAssert.assertThat(jdbcUrl, CoreMatchers.containsString("&"));
			MatcherAssert.assertThat(jdbcUrl, CoreMatchers.containsString("sslmode=disable"));
			MatcherAssert.assertThat(jdbcUrl, CoreMatchers.containsString("application_name=yugabyte"));
		}
	}

	@Test
	public void testWithCustomRole() throws SQLException {
		try (final YugabyteDBYSQLContainer ysqlContainer = new YugabyteDBYSQLContainer(YBDB_TEST_IMAGE)
				.withDatabaseName("yugabyte").withPassword("yugabyte").withUsername("yugabyte")) {
			ysqlContainer.start();
			Assert.assertEquals("Query execution with a custom role fails!", 1,
					performQuery(ysqlContainer, "SELECT 1").getInt(1));
		}
	}

}
