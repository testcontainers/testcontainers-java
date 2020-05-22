package org.testcontainers.jdbc;

import org.junit.AfterClass;
import org.junit.Test;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.rnorth.visibleassertions.VisibleAssertions.assertNotNull;
import static org.rnorth.visibleassertions.VisibleAssertions.assertNull;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

/**
 * This test belongs in the jdbc module, as it is focused on testing the behaviour of {@link org.testcontainers.containers.JdbcDatabaseContainer}.
 * However, the need to use the {@link org.testcontainers.containers.PostgreSQLContainerProvider} (due to the jdbc:tc:postgresql) URL forces it to live here in
 * the mysql module, to avoid circular dependencies.
 * TODO: Move to the jdbc module and either (a) implement a barebones {@link org.testcontainers.containers.JdbcDatabaseContainerProvider} for testing, or (b) refactor into a unit test.
 */
public class DatabaseDriverShutdownTest {
    @AfterClass
    public static void testCleanup() {
        ContainerDatabaseDriver.killContainers();
    }

    @Test
    public void shouldStopContainerWhenAllConnectionsClosed() throws SQLException {
        final String jdbcUrl = "jdbc:tc:postgresql:9.6.8://hostname/databasename";

        getConnectionAndClose(jdbcUrl);

        JdbcDatabaseContainer<?> container = ContainerDatabaseDriver.getContainer(jdbcUrl);
        assertNull("Database container instance is null as expected", container);
    }

    @Test
    public void shouldNotStopDaemonContainerWhenAllConnectionsClosed() throws SQLException {
        final String jdbcUrl = "jdbc:tc:postgresql:9.6.8://hostname/databasename?TC_DAEMON=true";

        getConnectionAndClose(jdbcUrl);

        JdbcDatabaseContainer<?> container = ContainerDatabaseDriver.getContainer(jdbcUrl);
        assertNotNull("Database container instance is not null as expected", container);
        assertTrue("Database container is running as expected", container.isRunning());
    }

    private void getConnectionAndClose(String jdbcUrl) throws SQLException {
        try (Connection connection = DriverManager.getConnection(jdbcUrl)) {
            assertNotNull("Obtained connection as expected", connection);
        }
    }
}
