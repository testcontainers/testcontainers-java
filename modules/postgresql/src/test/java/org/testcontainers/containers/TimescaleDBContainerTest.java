package org.testcontainers.containers;

import org.junit.jupiter.api.Test;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.SQLException;

class TimescaleDBContainerTest extends AbstractContainerDatabaseTest {

    @Test
    void testSimple() throws SQLException {
        try (JdbcDatabaseContainer<?> postgres = new TimescaleDBContainerProvider().newInstance()) {
            postgres.start();

            executeSelectOneQuery(postgres);
        }
    }

    @Test
    void testCommandOverride() throws SQLException {
        try (
            GenericContainer<?> postgres = new TimescaleDBContainerProvider()
                .newInstance()
                .withCommand("postgres -c max_connections=42")
        ) {
            postgres.start();

            executeSelectMaxConnectionsQuery((JdbcDatabaseContainer<?>) postgres, "42");
        }
    }

    @Test
    void testUnsetCommand() throws SQLException {
        try (
            GenericContainer<?> postgres = new TimescaleDBContainerProvider()
                .newInstance()
                .withCommand("postgres -c max_connections=42")
                .withCommand()
        ) {
            postgres.start();

            executeSelectMaxConnectionsQuery((JdbcDatabaseContainer<?>) postgres, "100");
        }
    }

    @Test
    void testExplicitInitScript() throws SQLException {
        try (
            JdbcDatabaseContainer<?> postgres = new TimescaleDBContainerProvider()
                .newInstance()
                .withInitScript("somepath/init_timescaledb.sql")
        ) {
            postgres.start();

            executeSelectFooBarQuery(postgres);
        }
    }
}
