package org.testcontainers.junit.yugabytedb;

import org.junit.Test;
import org.testcontainers.containers.YugabyteDBYSQLContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.utility.DockerImageName;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * YugabyteDB YSQL API unit test class
 */
public class YugabyteDBYSQLTest extends AbstractContainerDatabaseTest {

    private static final String IMAGE_NAME = "yugabytedb/yugabyte:2.18.1.0-b84";

    private static final DockerImageName YBDB_TEST_IMAGE = DockerImageName.parse(IMAGE_NAME);

    @Test
    public void testSmoke() throws SQLException {
        try (
            // creatingYSQLContainer {
            final YugabyteDBYSQLContainer ysqlContainer = new YugabyteDBYSQLContainer(
                "yugabytedb/yugabyte:2.18.1.0-b84"
            )
            // }
        ) {
            // startingYSQLContainer {
            ysqlContainer.start();
            // }
            assertThat(performQuery(ysqlContainer, "SELECT 1").getInt(1))
                .as("A sample test query succeeds")
                .isEqualTo(1);
        }
    }

    @Test
    public void testCustomDatabase() throws SQLException {
        String key = "random";
        try (
            final YugabyteDBYSQLContainer ysqlContainer = new YugabyteDBYSQLContainer(YBDB_TEST_IMAGE)
                .withDatabaseName(key)
        ) {
            ysqlContainer.start();
            assertThat(performQuery(ysqlContainer, "SELECT 1").getInt(1))
                .as("A test query on a custom database succeeds")
                .isEqualTo(1);
        }
    }

    @Test
    public void testInitScript() throws SQLException {
        try (
            final YugabyteDBYSQLContainer ysqlContainer = new YugabyteDBYSQLContainer(YBDB_TEST_IMAGE)
                .withInitScript("init/init_yql.sql")
        ) {
            ysqlContainer.start();
            assertThat(performQuery(ysqlContainer, "SELECT greet FROM dsql").getString(1))
                .as("A record match succeeds")
                .isEqualTo("Hello DSQL");
        }
    }

    @Test
    public void testWithAdditionalUrlParamInJdbcUrl() {
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
    public void testWithCustomRole() throws SQLException {
        try (
            final YugabyteDBYSQLContainer ysqlContainer = new YugabyteDBYSQLContainer(YBDB_TEST_IMAGE)
                .withDatabaseName("yugabyte")
                .withPassword("yugabyte")
                .withUsername("yugabyte")
        ) {
            ysqlContainer.start();
            assertThat(performQuery(ysqlContainer, "SELECT 1").getInt(1))
                .as("A sample test query with a custom role succeeds")
                .isEqualTo(1);
        }
    }
}
