package org.testcontainers.cockroachdb;

import org.junit.jupiter.api.Test;
import org.testcontainers.CockroachDBTestImages;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.images.builder.Transferable;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.LogManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CockroachContainerTest extends AbstractContainerDatabaseTest {
    static {
        // Postgres JDBC driver uses JUL; disable it to avoid annoying, irrelevant, stderr logs during connection testing
        LogManager.getLogManager().getLogger("").setLevel(Level.OFF);
    }

    @Test
    void testSimple() throws SQLException {
        try ( // container {
            CockroachContainer cockroach = new CockroachContainer("cockroachdb/cockroach:v22.2.3")
            // }
        ) {
            cockroach.start();

            ResultSet resultSet = performQuery(cockroach, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
        }
    }

    @Test
    void testExplicitInitScript() throws SQLException {
        try (
            CockroachContainer cockroach = new CockroachContainer(CockroachDBTestImages.COCKROACHDB_IMAGE)
                .withInitScript("somepath/init_postgresql.sql")
        ) { // CockroachDB is expected to be compatible with Postgres
            cockroach.start();

            ResultSet resultSet = performQuery(cockroach, "SELECT foo FROM bar");

            String firstColumnValue = resultSet.getString(1);
            assertThat(firstColumnValue).as("Value from init script should equal real value").isEqualTo("hello world");
        }
    }

    @Test
    void testWithAdditionalUrlParamInJdbcUrl() {
        CockroachContainer cockroach = new CockroachContainer(CockroachDBTestImages.COCKROACHDB_IMAGE)
            .withUrlParam("sslmode", "disable")
            .withUrlParam("application_name", "cockroach");

        try {
            cockroach.start();
            String jdbcUrl = cockroach.getJdbcUrl();
            assertThat(jdbcUrl)
                .contains("?")
                .contains("&")
                .contains("sslmode=disable")
                .contains("application_name=cockroach");
        } finally {
            cockroach.stop();
        }
    }

    @Test
    void testWithUsernamePasswordDatabase() throws SQLException {
        try (
            CockroachContainer cockroach = new CockroachContainer(
                CockroachDBTestImages.FIRST_COCKROACHDB_IMAGE_WITH_ENV_VARS_SUPPORT
            )
                .withUsername("test_user")
                .withPassword("test_password")
                .withDatabaseName("test_database")
        ) {
            cockroach.start();

            ResultSet resultSet = performQuery(cockroach, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);

            String jdbcUrl = cockroach.getJdbcUrl();
            assertThat(jdbcUrl).contains("/" + "test_database");
        }
    }

    @Test
    void testAnExceptionIsThrownWhenImageDoesNotSupportEnvVars() {
        CockroachContainer cockroachContainer = new CockroachContainer(
            CockroachDBTestImages.COCKROACHDB_IMAGE_WITH_ENV_VARS_UNSUPPORTED
        );

        assertThatThrownBy(() -> cockroachContainer.withUsername("test_user"))
            .isInstanceOf(UnsupportedOperationException.class)
            .withFailMessage("Setting a username in not supported in the versions below 22.1.0");

        assertThatThrownBy(() -> cockroachContainer.withPassword("test_password"))
            .isInstanceOf(UnsupportedOperationException.class)
            .withFailMessage("Setting a password in not supported in the versions below 22.1.0");

        assertThatThrownBy(() -> cockroachContainer.withDatabaseName("test_database"))
            .isInstanceOf(UnsupportedOperationException.class)
            .withFailMessage("Setting a databaseName in not supported in the versions below 22.1.0");
    }

    @Test
    void testInitializationScript() throws SQLException {
        String sql =
            "USE postgres; \n" +
            "CREATE TABLE bar (foo VARCHAR(255)); \n" +
            "INSERT INTO bar (foo) VALUES ('hello world');";

        try (
            CockroachContainer cockroach = new CockroachContainer(CockroachDBTestImages.COCKROACHDB_IMAGE)
                .withCopyToContainer(Transferable.of(sql), "/docker-entrypoint-initdb.d/init.sql")
                .withLogConsumer(outputFrame -> System.out.println(outputFrame.getUtf8String()))
        ) { // CockroachDB is expected to be compatible with Postgres
            cockroach.start();

            ResultSet resultSet = performQuery(cockroach, "SELECT foo FROM bar");

            String firstColumnValue = resultSet.getString(1);
            assertThat(firstColumnValue).as("Value from init script should equal real value").isEqualTo("hello world");
        }
    }
}
