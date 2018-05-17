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
 * Created by inikolaev on 08/06/2017.
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
