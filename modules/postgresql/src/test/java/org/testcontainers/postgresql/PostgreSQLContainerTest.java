package org.testcontainers.postgresql;

import org.junit.jupiter.api.Test;
import org.testcontainers.PostgreSQLTestImages;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.LogManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class PostgreSQLContainerTest extends AbstractContainerDatabaseTest {
    static {
        // Postgres JDBC driver uses JUL; disable it to avoid annoying, irrelevant, stderr logs during connection testing
        LogManager.getLogManager().getLogger("").setLevel(Level.OFF);
    }

    @Test
    void testSimple() throws SQLException {
        try ( // container {
            PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:9.6.12")
            // }
        ) {
            postgres.start();

            performQuery(
                postgres,
                "SELECT 1",
                resultSet -> {
                    assertThatNoException()
                        .isThrownBy(() -> {
                            int resultSetInt = resultSet.getInt(1);
                            assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
                            assertHasCorrectExposedAndLivenessCheckPorts(postgres);
                        });
                }
            );
        }
    }

    @Test
    void testCommandOverride() throws SQLException {
        try (
            PostgreSQLContainer postgres = new PostgreSQLContainer(PostgreSQLTestImages.POSTGRES_TEST_IMAGE)
                .withCommand("postgres -c max_connections=42")
        ) {
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
            PostgreSQLContainer postgres = new PostgreSQLContainer(PostgreSQLTestImages.POSTGRES_TEST_IMAGE)
                .withCommand("postgres -c max_connections=42")
                .withCommand()
        ) {
            postgres.start();

            performQuery(
                postgres,
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
    void testMissingInitScript() {
        try (
            PostgreSQLContainer postgres = new PostgreSQLContainer(PostgreSQLTestImages.POSTGRES_TEST_IMAGE)
                .withInitScript(null)
        ) {
            assertThatNoException().isThrownBy(postgres::start);
        }
    }

    @Test
    void testExplicitInitScript() throws SQLException {
        try (
            PostgreSQLContainer postgres = new PostgreSQLContainer(PostgreSQLTestImages.POSTGRES_TEST_IMAGE)
                .withInitScript("somepath/init_postgresql.sql")
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

    @Test
    void testExplicitInitScripts() throws SQLException {
        try (
            PostgreSQLContainer postgres = new PostgreSQLContainer(PostgreSQLTestImages.POSTGRES_TEST_IMAGE)
                .withInitScripts("somepath/init_postgresql.sql", "somepath/init_postgresql_2.sql")
        ) {
            postgres.start();

            performQuery(
                postgres,
                "SELECT foo AS value FROM bar UNION SELECT bar AS value FROM foo",
                resultSet -> {
                    assertThatNoException()
                        .isThrownBy(() -> {
                            String columnValue1 = resultSet.getString(1);
                            resultSet.next();
                            String columnValue2 = resultSet.getString(1);
                            assertThat(columnValue1)
                                .as("Value from init script 1 should equal real value")
                                .isEqualTo("hello world");
                            assertThat(columnValue2)
                                .as("Value from init script 2 should equal real value")
                                .isEqualTo("hello world 2");
                        });
                }
            );
        }
    }

    @Test
    void testWithAdditionalUrlParamInJdbcUrl() {
        try (
            PostgreSQLContainer postgres = new PostgreSQLContainer(PostgreSQLTestImages.POSTGRES_TEST_IMAGE)
                .withUrlParam("charSet", "UNICODE")
        ) {
            postgres.start();
            String jdbcUrl = postgres.getJdbcUrl();
            assertThat(jdbcUrl).contains("?");
            assertThat(jdbcUrl).contains("&");
            assertThat(jdbcUrl).contains("charSet=UNICODE");
        }
    }

    private void assertHasCorrectExposedAndLivenessCheckPorts(PostgreSQLContainer postgres) {
        assertThat(postgres.getExposedPorts()).containsExactly(PostgreSQLContainer.POSTGRESQL_PORT);
        assertThat(postgres.getLivenessCheckPortNumbers())
            .containsExactly(postgres.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT));
    }
}
