package org.testcontainers.junit.yugabytedb;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.YugabyteDBYSQLContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.utility.DockerImageName;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

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
            final YugabyteDBYSQLContainer ysqlContainer = new YugabyteDBYSQLContainer(
                "yugabytedb/yugabyte:2.14.4.0-b26"
            )
            // }
        ) {
            ysqlContainer.start();

            executeSelectOneQuery(ysqlContainer);
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

            executeSelectOneQuery(ysqlContainer);
        }
    }

    @Test
    void testInitScript() throws SQLException {
        try (
            final YugabyteDBYSQLContainer ysqlContainer = new YugabyteDBYSQLContainer(YBDB_TEST_IMAGE)
                .withInitScript("init/init_yql.sql")
        ) {
            ysqlContainer.start();
            executeQuery(
                ysqlContainer,
                "SELECT greet FROM dsql",
                resultSet -> {
                    Assertions
                        .assertThatNoException()
                        .isThrownBy(() -> {
                            assertThat(resultSet.getString(1)).as("A record match succeeds").isEqualTo("Hello DSQL");
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

            executeSelectOneQuery(ysqlContainer);
        }
    }

    @Test
    void testWaitStrategy() throws SQLException {
        try (final YugabyteDBYSQLContainer ysqlContainer = new YugabyteDBYSQLContainer(YBDB_TEST_IMAGE)) {
            ysqlContainer.start();

            executeSelectOneQuery(ysqlContainer);

            executeQuery(
                ysqlContainer,
                "SELECT EXISTS (SELECT FROM pg_tables WHERE tablename = 'YB_SAMPLE')",
                resultSet -> {
                    Assertions
                        .assertThatNoException()
                        .isThrownBy(() -> {
                            boolean tableExists = resultSet.getBoolean(1);
                            assertThat(tableExists).as("yb_sample table does not exists").isFalse();
                        });
                }
            );
        }
    }
}
