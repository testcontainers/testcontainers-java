package org.testcontainers.jdbc;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

public class ContainerDatabaseDriverTest {

    private static final String PLAIN_POSTGRESQL_JDBC_URL = "jdbc:postgresql://localhost:5432/test";

    @Test
    public void shouldNotTryToConnectToNonMatchingJdbcUrlDirectly() throws SQLException {
        ContainerDatabaseDriver driver = new ContainerDatabaseDriver();
        Connection connection = driver.connect(PLAIN_POSTGRESQL_JDBC_URL, new Properties());
        assertThat(connection).isNull();
    }

    @Test
    public void shouldNotTryToConnectToNonMatchingJdbcUrlViaDriverManager() throws SQLException {
        SQLException e = catchThrowableOfType(SQLException.class, () ->
                DriverManager.getConnection(PLAIN_POSTGRESQL_JDBC_URL)
        );
        assertThat((Throwable) e)
            .hasMessageStartingWith("No suitable driver found for ");
    }
}
