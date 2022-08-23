package org.testcontainers.junit.cockroachdb;

import org.junit.Test;
import org.testcontainers.CockroachDBTestImages;
import org.testcontainers.containers.CockroachContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.LogManager;

import static org.assertj.core.api.Assertions.assertThat;

public class SimpleCockroachDBTest extends AbstractContainerDatabaseTest {
    static {
        // Postgres JDBC driver uses JUL; disable it to avoid annoying, irrelevant, stderr logs during connection testing
        LogManager.getLogManager().getLogger("").setLevel(Level.OFF);
    }

    @Test
    public void testSimple() throws SQLException {
        try (CockroachContainer cockroach = new CockroachContainer(CockroachDBTestImages.COCKROACHDB_IMAGE)) {
            cockroach.start();

            ResultSet resultSet = performQuery(cockroach, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
        }
    }

    @Test
    public void testExplicitInitScript() throws SQLException {
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
    public void testWithAdditionalUrlParamInJdbcUrl() {
        CockroachContainer cockroach = new CockroachContainer(CockroachDBTestImages.COCKROACHDB_IMAGE)
            .withUrlParam("sslmode", "disable")
            .withUrlParam("application_name", "cockroach");

        try {
            cockroach.start();
            String jdbcUrl = cockroach.getJdbcUrl();
            assertThat(jdbcUrl).contains("?");
            assertThat(jdbcUrl).contains("&");
            assertThat(jdbcUrl).contains("sslmode=disable");
            assertThat(jdbcUrl).contains("application_name=cockroach");
        } finally {
            cockroach.stop();
        }
    }
}
