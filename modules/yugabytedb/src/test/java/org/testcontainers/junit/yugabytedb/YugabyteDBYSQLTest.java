package org.testcontainers.junit.yugabytedb;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.YugabyteDBYSQLContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.utility.DockerImageName;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

/**
 * YugabyteDB YSQL API unit test class
 */
class YugabyteDBYSQLTest extends AbstractContainerDatabaseTest {

    private static final String IMAGE_NAME = "yugabytedb/yugabyte:2.14.4.0-b26";

    private static final DockerImageName YBDB_TEST_IMAGE = DockerImageName.parse(IMAGE_NAME);

    @Test
    void testSmoke() throws SQLException {
        try (
            // creatingYSQLContainer {
            final YugabyteDBYSQLContainer ysqlContainer = new YugabyteDBYSQLContainer(YBDB_TEST_IMAGE)
            // }
        ) {
            ysqlContainer.start();
            performQuery(
                ysqlContainer,
                "SELECT 1",
                resultSet -> {
                    assertThatNoException()
                        .isThrownBy(() -> {
                            int resultSetInt = resultSet.getInt(1);
                            assertThat(resultSetInt).as("A sample test query succeeds").isEqualTo(1);
                        });
                }
            );
        }
    }

    @Test
    void testCustomDatabase() throws SQLException {
        String key = "random";
        try (
            final YugabyteDBYSQLContainer ysqlContainer = new YugabyteDBYSQLContainer(YBDB_TEST_IMAGE)
                .withDatabaseName(key)
        ) {
            ysqlContainer.start();
            performQuery(
                ysqlContainer,
                "SELECT 1",
                resultSet -> {
                    assertThatNoException()
                        .isThrownBy(() -> {
                            int resultSetInt = resultSet.getInt(1);
                            assertThat(resultSetInt).as("A test query on a custom database succeeds").isEqualTo(1);
                        });
                }
            );
        }
    }

    @Test
    void testInitScript() throws SQLException {
        try (
            final YugabyteDBYSQLContainer ysqlContainer = new YugabyteDBYSQLContainer(YBDB_TEST_IMAGE)
                .withInitScript("init/init_yql.sql")
        ) {
            ysqlContainer.start();
            performQuery(
                ysqlContainer,
                "SELECT greet FROM dsql",
                resultSet -> {
                    assertThatNoException()
                        .isThrownBy(() -> {
                            String resultSetString = resultSet.getString(1);
                            assertThat(resultSetString).as("A record match succeeds").isEqualTo("Hello DSQL");
                        });
                }
            );
        }
    }

    @Test
    void testWithAdditionalUrlParamInJdbcUrl() {
        try (
            final YugabyteDBYSQLContainer ysqlContainer = new YugabyteDBYSQLContainer(YBDB_TEST_IMAGE)
                .withUrlParam("sslmode", "disable")
                .withUrlParam("application_name", "yugabyte")
        ) {
            ysqlContainer.start();
            String jdbcUrl = ysqlContainer.getJdbcUrl();
            assertThat(jdbcUrl)
                .contains("?")
                .contains("&")
                .contains("sslmode=disable")
                .contains("application_name=yugabyte")
                .as("A JDBC connection string with additional parameter validation succeeds");
        }
    }

    @Test
    void testWithCustomRole() throws SQLException {
        try (
            final YugabyteDBYSQLContainer ysqlContainer = new YugabyteDBYSQLContainer(YBDB_TEST_IMAGE)
                .withDatabaseName("yugabyte")
                .withPassword("yugabyte")
                .withUsername("yugabyte")
        ) {
            ysqlContainer.start();
            performQuery(
                ysqlContainer,
                "SELECT 1",
                resultSet -> {
                    assertThatNoException()
                        .isThrownBy(() -> {
                            int resultSetInt = resultSet.getInt(1);
                            assertThat(resultSetInt).as("A sample test query with a custom role succeeds").isEqualTo(1);
                        });
                }
            );
        }
    }

    @Test
    void testWaitStrategy() throws SQLException {
        try (final YugabyteDBYSQLContainer ysqlContainer = new YugabyteDBYSQLContainer(YBDB_TEST_IMAGE)) {
            ysqlContainer.start();

            performQuery(
                ysqlContainer,
                "SELECT 1",
                resultSet -> {
                    assertThatNoException()
                        .isThrownBy(() -> {
                            int resultSetInt = resultSet.getInt(1);
                            assertThat(resultSetInt).as("A sample test query succeeds").isEqualTo(1);
                        });
                }
            );

            performQuery(
                ysqlContainer,
                "SELECT EXISTS (SELECT FROM pg_tables WHERE tablename = 'YB_SAMPLE')",
                resultSet -> {
                    assertThatNoException()
                        .isThrownBy(() -> {
                            boolean tableExists = resultSet.getBoolean(1);
                            assertThat(tableExists).as("A sample test query succeeds").isFalse();
                        });
                }
            );
        }
    }
}
