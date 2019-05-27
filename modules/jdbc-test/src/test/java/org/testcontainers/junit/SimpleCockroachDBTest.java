package org.testcontainers.junit;

import org.junit.Test;
import org.testcontainers.containers.CockroachContainer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.LogManager;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

public class SimpleCockroachDBTest extends AbstractContainerDatabaseTest {

    static {
        // Postgres JDBC driver uses JUL; disable it to avoid annoying, irrelevant, stderr logs during connection testing
        LogManager.getLogManager().getLogger("").setLevel(Level.OFF);
    }

    @Test
    public void testSimple() throws SQLException {
        try (CockroachContainer cockroach = new CockroachContainer()) {
            cockroach.start();

            ResultSet resultSet = performQuery(cockroach, "SELECT 1");

            int resultSetInt = resultSet.getInt(1);
            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
        }
    }

    @Test
    public void testExplicitInitScript() throws SQLException {
        try (CockroachContainer cockroach = new CockroachContainer()
                .withInitScript("somepath/init_postgresql.sql")) { // CockroachDB is expected to be compatible with Postgres
            cockroach.start();

            ResultSet resultSet = performQuery(cockroach, "SELECT foo FROM bar");

            String firstColumnValue = resultSet.getString(1);
            assertEquals("Value from init script should equal real value", "hello world", firstColumnValue);
        }
    }
}
