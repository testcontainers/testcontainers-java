package org.testcontainers.jdbc;

import org.junit.Test;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertNotEquals;

public class DatabaseDriverTmpfsTest {

    @Test
    public void testDatabaseHasTmpFsViaConnectionString() throws Exception {
        final String jdbcUrl = "jdbc:tc:postgresql:9.6.8://hostname/databasename?TC_TMPFS=/testtmpfs:rw";
        try (Connection ignored = DriverManager.getConnection(jdbcUrl)) {

            JdbcDatabaseContainer<?> container = ContainerDatabaseDriver.getContainer(jdbcUrl);
            // check file doesn't exist
            String path = "/testtmpfs/test.file";
            Container.ExecResult execResult = container.execInContainer("ls", path);
            assertNotEquals("tmpfs inside container doesn't have file that doesn't exist", 0, execResult.getExitCode());
            // touch && check file does exist
            container.execInContainer("touch", path);
            execResult = container.execInContainer("ls", path);
            assertEquals("tmpfs inside container has file that does exist", 0, execResult.getExitCode());
        }
    }
}
