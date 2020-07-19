package org.testcontainers.junit.cockroachdb;

import org.junit.Test;
import org.testcontainers.containers.CockroachContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.LogManager;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.testcontainers.CockroachDBTestImages.COCKROACHDB_IMAGE;

public class SimpleCockroachDBTest extends AbstractContainerDatabaseTest {

    static {
        // Postgres JDBC driver uses JUL; disable it to avoid annoying, irrelevant, stderr logs during connection testing
        LogManager.getLogManager().getLogger("").setLevel(Level.OFF);
    }

    @Test
    public void testSimple() throws SQLException {
        try (CockroachContainer cockroach = new CockroachContainer(COCKROACHDB_IMAGE)) {
            cockroach.start();

            ResultSet resultSet = performQuery(cockroach, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
        }
    }

    @Test
    public void testExplicitInitScript() throws SQLException {
        try (CockroachContainer cockroach = new CockroachContainer(COCKROACHDB_IMAGE)
                .withInitScript("somepath/init_postgresql.sql")) { // CockroachDB is expected to be compatible with Postgres
            cockroach.start();

            ResultSet resultSet = performQuery(cockroach, "SELECT foo FROM bar");

            String firstColumnValue = resultSet.getString(1);
            assertEquals("Value from init script should equal real value", "hello world", firstColumnValue);
        }
    }

    @Test
    public void testWithAdditionalUrlParamInJdbcUrl() {
        CockroachContainer cockroach = new CockroachContainer(COCKROACHDB_IMAGE)
            .withUrlParam("sslmode", "disable")
            .withUrlParam("application_name", "cockroach");

        try {
            cockroach.start();
            String jdbcUrl = cockroach.getJdbcUrl();
            assertThat(jdbcUrl, containsString("?"));
            assertThat(jdbcUrl, containsString("&"));
            assertThat(jdbcUrl, containsString("sslmode=disable"));
            assertThat(jdbcUrl, containsString("application_name=cockroach"));
        } finally {
            cockroach.stop();
        }
    }
}
