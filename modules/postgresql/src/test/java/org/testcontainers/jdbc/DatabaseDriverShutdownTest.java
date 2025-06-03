package org.testcontainers.jdbc;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test belongs in the jdbc module, as it is focused on testing the behaviour of {@link org.testcontainers.containers.JdbcDatabaseContainer}.
 * However, the need to use the {@link org.testcontainers.containers.PostgreSQLContainerProvider} (due to the jdbc:tc:postgresql) URL forces it to live here in
 * the mysql module, to avoid circular dependencies.
 * TODO: Move to the jdbc module and either (a) implement a barebones {@link org.testcontainers.containers.JdbcDatabaseContainerProvider} for testing, or (b) refactor into a unit test.
 */
public class DatabaseDriverShutdownTest {

    @AfterAll
    public static void testCleanup() {
        ContainerDatabaseDriver.killContainers();
    }

    @Test
    public void shouldStopContainerWhenAllConnectionsClosed() throws SQLException {
        final String jdbcUrl = "jdbc:tc:postgresql:9.6.8://hostname/databasename";

        getConnectionAndClose(jdbcUrl);

        JdbcDatabaseContainer<?> container = ContainerDatabaseDriver.getContainer(jdbcUrl);
        assertThat(container).as("Database container instance is null as expected").isNull();
    }

    @Test
    public void shouldNotStopDaemonContainerWhenAllConnectionsClosed() throws SQLException {
        final String jdbcUrl = "jdbc:tc:postgresql:9.6.8://hostname/databasename?TC_DAEMON=true";

        getConnectionAndClose(jdbcUrl);

        JdbcDatabaseContainer<?> container = ContainerDatabaseDriver.getContainer(jdbcUrl);
        assertThat(container).as("Database container instance is not null as expected").isNotNull();
        assertThat(container.isRunning()).as("Database container is running as expected").isTrue();
    }

    private void getConnectionAndClose(String jdbcUrl) throws SQLException {
        try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
            assertThat(connection).as("Obtained connection as expected").isNotNull();
        }
    }
}
