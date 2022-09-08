package org.testcontainers.jdbc;

import org.hamcrest.CoreMatchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

public class ContainerDatabaseDriverTest {

    private static final String PLAIN_POSTGRESQL_JDBC_URL = "jdbc:postgresql://localhost:5432/test";

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void shouldNotTryToConnectToNonMatchingJdbcUrlDirectly() throws SQLException {
        ContainerDatabaseDriver driver = new ContainerDatabaseDriver();
        Connection connection = driver.connect(PLAIN_POSTGRESQL_JDBC_URL, new Properties());
        assertThat(connection).isNull();
    }

    @Test
    public void shouldNotTryToConnectToNonMatchingJdbcUrlViaDriverManager() throws SQLException {
        thrown.expect(SQLException.class);
        thrown.expectMessage(CoreMatchers.startsWith("No suitable driver found for "));
        DriverManager.getConnection(PLAIN_POSTGRESQL_JDBC_URL);
    }

    @Test
    public void shouldRespectBothUrlPropertiesAndParameterProperties() throws SQLException {
        ContainerDatabaseDriver driver = new ContainerDatabaseDriver();
        String url = "jdbc:tc:mysql:5.7.22://hostname/databasename?padCharsWithSpace=true";
        Properties properties = new Properties();
        properties.setProperty("maxRows", "1");

        try (Connection connection = driver.connect(url, properties)) {
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE arbitrary_table (length_5_string CHAR(5))");
                statement.execute("INSERT INTO arbitrary_table VALUES ('abc')");
                statement.execute("INSERT INTO arbitrary_table VALUES ('123')");

                // Check that maxRows is set
                try (ResultSet resultSet = statement.executeQuery("SELECT * FROM arbitrary_table")) {
                    assertThat(resultSet.next()).isTrue();
                    assertThat(resultSet.next()).isFalse();
                }

                // Check that pad with chars is set
                try (ResultSet resultSet = statement.executeQuery("SELECT * FROM arbitrary_table")) {
                    assertThat(resultSet.next()).isTrue();
                    assertThat(resultSet.getString(1)).isEqualTo("abc  ");
                }
            }
        }
    }
}
