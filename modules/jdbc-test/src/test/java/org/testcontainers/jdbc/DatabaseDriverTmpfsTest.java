package org.testcontainers.jdbc;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import org.junit.Test;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.JdbcDatabaseContainer;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

public class DatabaseDriverTmpfsTest {

    @Test
    public void tmpfs() throws IOException, InterruptedException, SQLException {
        final String jdbcUrl = "jdbc:tc:postgresql:9.6.8://hostname/databasename?TC_TMPFS=/testtmpfs:rw";
        try (Connection ignored = DriverManager.getConnection(jdbcUrl)) {

            JdbcDatabaseContainer<?> container = ContainerDatabaseDriver.getContainer(jdbcUrl);
            // check file doesn't exist
            String path = "/testtmpfs/test.file";
            Container.ExecResult execResult = container.execInContainer("ls", path);
            assertEquals("tmpfs inside container works fine", execResult.getStderr(),
                "ls: cannot access '/testtmpfs/test.file': No such file or directory\n");
            // touch && check file does exist
            container.execInContainer("touch", path);
            execResult = container.execInContainer("ls", path);
            assertEquals("tmpfs inside container works fine", execResult.getStdout(), path + "\n");
        }

    }


}
