package org.testcontainers.junit.yugabytedb;

import java.sql.SQLException;

import org.junit.Test;
import org.testcontainers.containers.YugabyteYSQLContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.testcontainers.YugabyteTestContainerConstants.IMAGE_NAME;
import static org.testcontainers.YugabyteTestContainerConstants.YBDB_TEST_IMAGE;

/**
 * YugabyteDB YSQL API unit test class
 *
 * @author srinivasa-vasu
 */
public class YugabyteYSQLUnitTest extends AbstractContainerDatabaseTest {

	@Test
	public void testSmoke() throws SQLException {
		try (YugabyteYSQLContainer container = new YugabyteYSQLContainer(IMAGE_NAME)) {
			container.start();
			assertEquals("Query execution fails!", 1, performQuery(container, "SELECT 1").getInt(1));
		}
	}

	@Test
	public void testCustomDatabase() throws SQLException {
		String key = "random";
		try (YugabyteYSQLContainer container = new YugabyteYSQLContainer(YBDB_TEST_IMAGE).withDatabaseName(key)) {
			container.start();
			assertEquals("Query execution on a custom database fails!", 1,
					performQuery(container, "SELECT 1").getInt(1));
		}
	}

	@Test
	public void testExplicitInitScript() throws SQLException {
		try (YugabyteYSQLContainer container = new YugabyteYSQLContainer(YBDB_TEST_IMAGE)
				.withInitScript("init/init_yql.sql")) {
			container.start();
			assertEquals("Value from the init script does not match the real value", "hello world",
					performQuery(container, "SELECT foo FROM bar").getString(1));
		}
	}

	@Test
	public void testWithAdditionalUrlParamInJdbcUrl() {
		try (YugabyteYSQLContainer container = new YugabyteYSQLContainer(YBDB_TEST_IMAGE)
				.withUrlParam("sslmode", "disable").withUrlParam("application_name", "yugabyte")) {
			container.start();
			String jdbcUrl = container.getJdbcUrl();
			assertThat(jdbcUrl, containsString("?"));
			assertThat(jdbcUrl, containsString("&"));
			assertThat(jdbcUrl, containsString("sslmode=disable"));
			assertThat(jdbcUrl, containsString("application_name=yugabyte"));
		}
	}

	@Test
	public void testWithCustomRole() throws SQLException {
		try (YugabyteYSQLContainer container = new YugabyteYSQLContainer(YBDB_TEST_IMAGE).withDatabaseName("yugabyte")
				.withPassword("yugabyte").withUsername("yugabyte")) {
			container.start();
			assertEquals("Query execution with a custom role fails!", 1, performQuery(container, "SELECT 1").getInt(1));
		}
	}

}
