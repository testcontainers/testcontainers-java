package org.testcontainers.jdbc;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ContainerDatabaseDriverTest {

    private static final String PLAIN_POSTGRESQL_JDBC_URL = "jdbc:postgresql://localhost:5432/test";

    @Test
    void shouldNotTryToConnectToNonMatchingJdbcUrlDirectly() throws SQLException {
        ContainerDatabaseDriver driver = new ContainerDatabaseDriver();
        Connection connection = driver.connect(PLAIN_POSTGRESQL_JDBC_URL, new Properties());
        assertThat(connection).isNull();
    }

    @Test
    void shouldNotTryToConnectToNonMatchingJdbcUrlViaDriverManager() throws SQLException {
        assertThatThrownBy(() -> DriverManager.getConnection(PLAIN_POSTGRESQL_JDBC_URL))
            .isInstanceOf(SQLException.class)
            .hasMessageStartingWith("No suitable driver found for ");
    }
}
