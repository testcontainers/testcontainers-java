package org.testcontainers.junit.postgresql;

import org.junit.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.LogManager;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertNotEquals;
import static org.testcontainers.PostgreSQLTestImages.POSTGRES_TEST_IMAGE;

public class SimplePostgreSQLTest extends AbstractContainerDatabaseTest {

    static {
        // Postgres JDBC driver uses JUL; disable it to avoid annoying, irrelevant, stderr logs during connection testing
        LogManager.getLogManager().getLogger("").setLevel(Level.OFF);
    }

    @Test
    public void testSimple() throws SQLException {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_TEST_IMAGE)) {
            postgres.start();

            ResultSet resultSet = performQuery(postgres, "SELECT 1");
            int resultSetInt = resultSet.getInt(1);
            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
        }
    }

    @Test
    public void testCommandOverride() throws SQLException {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_TEST_IMAGE).withCommand("postgres -c max_connections=42")) {
            postgres.start();

            ResultSet resultSet = performQuery(postgres, "SELECT current_setting('max_connections')");
            String result = resultSet.getString(1);
            assertEquals("max_connections should be overriden", "42", result);
        }
    }

    @Test
    public void testUnsetCommand() throws SQLException {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_TEST_IMAGE).withCommand("postgres -c max_connections=42").withCommand()) {
            postgres.start();

            ResultSet resultSet = performQuery(postgres, "SELECT current_setting('max_connections')");
            String result = resultSet.getString(1);
            assertNotEquals("max_connections should not be overriden", "42", result);
        }
    }

    @Test
    public void testExplicitInitScript() throws SQLException {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_TEST_IMAGE).withInitScript("somepath/init_postgresql.sql")) {
            postgres.start();

            ResultSet resultSet = performQuery(postgres, "SELECT foo FROM bar");

            String firstColumnValue = resultSet.getString(1);
            assertEquals("Value from init script should equal real value", "hello world", firstColumnValue);
        }
    }

    @Test
    public void testWithAdditionalUrlParamInJdbcUrl() {
        try (PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>(POSTGRES_TEST_IMAGE)
            .withUrlParam("charSet", "UNICODE")) {

            postgres.start();
            String jdbcUrl = postgres.getJdbcUrl();
            assertThat(jdbcUrl, containsString("?"));
            assertThat(jdbcUrl, containsString("&"));
            assertThat(jdbcUrl, containsString("charSet=UNICODE"));
        }
    }
}
