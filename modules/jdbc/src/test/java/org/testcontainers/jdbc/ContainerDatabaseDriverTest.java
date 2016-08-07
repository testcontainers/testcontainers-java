package org.testcontainers.jdbc;

import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class ContainerDatabaseDriverTest {

	private static final String PLAIN_POSTGRESQL_JDBC_URL = "jdbc:postgresql://localhost:5432/test";

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void shouldNotTryToConnectToNonMatchingJdbcUrlDirectly() throws SQLException {
		ContainerDatabaseDriver driver = new ContainerDatabaseDriver();
		Connection connection = driver.connect(PLAIN_POSTGRESQL_JDBC_URL, new Properties());
		Assert.assertNull(connection);
	}

	@Test
	public void shouldNotTryToConnectToNonMatchingJdbcUrlViaDriverManager() throws SQLException {
		thrown.expect(SQLException.class);
		thrown.expectMessage(CoreMatchers.startsWith("No suitable driver found for "));
		DriverManager.getConnection(PLAIN_POSTGRESQL_JDBC_URL);
	}

}
