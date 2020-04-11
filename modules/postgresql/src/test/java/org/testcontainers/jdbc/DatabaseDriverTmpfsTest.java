package org.testcontainers.jdbc;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.testsupport.Flaky;
import org.testcontainers.testsupport.FlakyTestJUnit4RetryRule;

import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

/**
 * This test belongs in the jdbc module, as it is focused on testing the behaviour of {@link org.testcontainers.containers.JdbcDatabaseContainer}.
 * However, the need to use the {@link org.testcontainers.containers.PostgreSQLContainerProvider} (due to the jdbc:tc:postgresql) URL forces it to live here in
 * the mysql module, to avoid circular dependencies.
 * TODO: Move to the jdbc module and either (a) implement a barebones {@link org.testcontainers.containers.JdbcDatabaseContainerProvider} for testing, or (b) refactor into a unit test.
 */
public class DatabaseDriverTmpfsTest {

    @Rule
    public FlakyTestJUnit4RetryRule retry = new FlakyTestJUnit4RetryRule();

    @Test
    @Flaky(githubIssueUrl = "https://github.com/testcontainers/testcontainers-java/issues/1687", reviewDate = "2020-06-01")
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
