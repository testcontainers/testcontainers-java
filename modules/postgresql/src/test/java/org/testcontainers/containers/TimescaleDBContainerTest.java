package org.testcontainers.containers;

import org.junit.jupiter.api.Test;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class TimescaleDBContainerTest extends AbstractContainerDatabaseTest {

    @Test
    void testSimple() throws SQLException {
        try (JdbcDatabaseContainer<?> postgres = new TimescaleDBContainerProvider().newInstance()) {
            postgres.start();

            performQuery(
                postgres,
                "SELECT 1",
                resultSet -> {
                    assertThatNoException()
                        .isThrownBy(() -> {
                            int resultSetInt = resultSet.getInt(1);
                            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
                        });
                }
            );
        }
    }

    @Test
    void testCommandOverride() throws SQLException {
        try (JdbcDatabaseContainer<?> postgres = new TimescaleDBContainerProvider().newInstance()) {
            postgres.withCommand("postgres -c max_connections=42");
            postgres.start();

            performQuery(
                postgres,
                "SELECT current_setting('max_connections')",
                resultSet -> {
                    assertThatNoException()
                        .isThrownBy(() -> {
                            String result = resultSet.getString(1);
                            assertThat(result).as("max_connections should be overridden").isEqualTo("42");
                        });
                }
            );
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

            performQuery(
                (JdbcDatabaseContainer<?>) postgres,
                "SELECT current_setting('max_connections')",
                resultSet -> {
                    assertThatNoException()
                        .isThrownBy(() -> {
                            String result = resultSet.getString(1);
                            assertThat(result).as("max_connections should not be overridden").isNotEqualTo("42");
                        });
                }
            );
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

            performQuery(
                postgres,
                "SELECT foo FROM bar",
                resultSet -> {
                    assertThatNoException()
                        .isThrownBy(() -> {
                            String firstColumnValue = resultSet.getString(1);
                            assertThat(firstColumnValue)
                                .as("Value from init script should equal real value")
                                .isEqualTo("hello world");
                        });
                }
            );
        }
    }
}
