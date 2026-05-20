package org.testcontainers.cockroachdb;

import org.junit.jupiter.api.Test;
import org.testcontainers.CockroachDBTestImages;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.images.builder.Transferable;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.LogManager;

import static org.assertj.core.api.Assertions.assertThat;

class CockroachContainerTest extends AbstractContainerDatabaseTest {
    static {
        // Postgres JDBC driver uses JUL; disable it to avoid annoying, irrelevant, stderr logs during connection testing
        LogManager.getLogManager().getLogger("").setLevel(Level.OFF);
    }

    @Test
    void testSimple() throws SQLException {
        try ( // container {
            CockroachContainer cockroach = new CockroachContainer("cockroachdb/cockroach:v26.1.1")
            // }
        ) {
            cockroach.start();
            executeSelectOneQuery(cockroach);
        }
    }

    @Test
    void testExplicitInitScript() throws SQLException {
        try (
            CockroachContainer cockroach = new CockroachContainer(CockroachDBTestImages.COCKROACHDB_IMAGE)
                .withInitScript("somepath/init_postgresql.sql")
        ) { // CockroachDB is expected to be compatible with Postgres
            cockroach.start();

            executeSelectFooBarQuery(cockroach);
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
            CockroachContainer cockroach = new CockroachContainer(CockroachDBTestImages.COCKROACHDB_IMAGE)
                .withUsername("test_user")
                .withPassword("test_password")
                .withDatabaseName("test_database")
        ) {
            cockroach.start();

            executeSelectOneQuery(cockroach);

            String jdbcUrl = cockroach.getJdbcUrl();
            assertThat(jdbcUrl).contains("/" + "test_database");
        }
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

            executeSelectFooBarQuery(cockroach);
        }
    }
}
