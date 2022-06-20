package org.testcontainers.junit.tidb;

import org.junit.Test;
import org.testcontainers.TiDBTestImages;
import org.testcontainers.containers.TiDBContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.LogManager;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;
import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

public class SimpleTiDBTest extends AbstractContainerDatabaseTest {

    static {
        // Postgres JDBC driver uses JUL; disable it to avoid annoying, irrelevant, stderr logs during connection testing
        LogManager.getLogManager().getLogger("").setLevel(Level.OFF);
    }

    @Test
    public void testSimple() throws SQLException {
        try (TiDBContainer tidb = new TiDBContainer(TiDBTestImages.TIDB_IMAGE)) {
            tidb.start();

            ResultSet resultSet = performQuery(tidb, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
        }
    }

    @Test
    public void testExplicitInitScript() throws SQLException {
        try (
            TiDBContainer cockroach = new TiDBContainer(TiDBTestImages.TIDB_IMAGE)
                .withInitScript("somepath/init_mysql.sql")
        ) { // CockroachDB is expected to be compatible with Postgres
            cockroach.start();

            ResultSet resultSet = performQuery(cockroach, "SELECT foo FROM bar");

            String firstColumnValue = resultSet.getString(1);
            assertEquals("Value from init script should equal real value", "hello world", firstColumnValue);
        }
    }

    @Test
    public void testWithAdditionalUrlParamInJdbcUrl() {
        TiDBContainer cockroach = new TiDBContainer(TiDBTestImages.TIDB_IMAGE)
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
