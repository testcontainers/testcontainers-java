package org.testcontainers.containers;

import org.junit.Test;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.SQLException;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertNotEquals;

public class TimescaleDBContainerTest extends AbstractContainerDatabaseTest {

    @Test
    public void testSimple() throws SQLException {
        try (JdbcDatabaseContainer<?> postgres = new TimescaleDBContainerProvider().newInstance()) {
            postgres.start();
            assertQuery(
                postgres,
                "SELECT 1",
                rs -> {
                    int resultSetInt = rs.getInt(1);
                    assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
                }
            );
        }
    }

    @Test
    public void testCommandOverride() throws SQLException {
        try (
            GenericContainer<?> postgres = new TimescaleDBContainerProvider()
                .newInstance()
                .withCommand("postgres -c max_connections=42")
        ) {
            postgres.start();

            assertQuery(
                (JdbcDatabaseContainer<?>) postgres,
                "SELECT current_setting('max_connections')",
                rs -> {
                    String result = rs.getString(1);
                    assertEquals("max_connections should be overriden", "42", result);
                }
            );
        }
    }

    @Test
    public void testUnsetCommand() throws SQLException {
        try (
            GenericContainer<?> postgres = new TimescaleDBContainerProvider()
                .newInstance()
                .withCommand("postgres -c max_connections=42")
                .withCommand()
        ) {
            postgres.start();

            assertQuery(
                (JdbcDatabaseContainer<?>) postgres,
                "SELECT current_setting('max_connections')",
                rs -> {
                    String result = rs.getString(1);
                    assertNotEquals("max_connections should not be overriden", "42", result);
                }
            );
        }
    }

    @Test
    public void testExplicitInitScript() throws SQLException {
        try (
            JdbcDatabaseContainer<?> postgres = new TimescaleDBContainerProvider()
                .newInstance()
                .withInitScript("somepath/init_timescaledb.sql")
        ) {
            postgres.start();

            assertQuery(
                postgres,
                "SELECT foo FROM bar",
                rs -> {
                    String firstColumnValue = rs.getString(1);
                    assertEquals("Value from init script should equal real value", "hello world", firstColumnValue);
                }
            );
        }
    }
}
