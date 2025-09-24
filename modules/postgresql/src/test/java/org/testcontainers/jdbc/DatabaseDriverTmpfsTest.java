package org.testcontainers.jdbc;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.JdbcDatabaseContainer;

import java.sql.Connection;
import java.sql.DriverManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * This test belongs in the jdbc module, as it is focused on testing the behaviour of {@link org.testcontainers.containers.JdbcDatabaseContainer}.
 * However, the need to use the {@link org.testcontainers.containers.PostgreSQLContainerProvider} (due to the jdbc:tc:postgresql) URL forces it to live here in
 * the mysql module, to avoid circular dependencies.
 * TODO: Move to the jdbc module and either (a) implement a barebones {@link org.testcontainers.containers.JdbcDatabaseContainerProvider} for testing, or (b) refactor into a unit test.
 */
class DatabaseDriverTmpfsTest {

    @Test
    void testDatabaseHasTmpFsViaConnectionString() throws Exception {
        final String jdbcUrl = "jdbc:tc:postgresql:9.6.8://hostname/databasename?TC_TMPFS=/testtmpfs:rw";
        try (Connection ignored = DriverManager.getConnection(jdbcUrl)) {
            JdbcDatabaseContainer<?> container = ContainerDatabaseDriver.getContainer(jdbcUrl);
            // check file doesn't exist
            String path = "/testtmpfs/test.file";
            Container.ExecResult execResult = container.execInContainer("ls", path);
            assertThat(execResult.getExitCode())
                .as("tmpfs inside container doesn't have file that doesn't exist")
                .isNotZero();
            // touch && check file does exist
            container.execInContainer("touch", path);
            execResult = container.execInContainer("ls", path);
            assertThat(execResult.getExitCode()).as("tmpfs inside container has file that does exist").isZero();
        }
    }
}
