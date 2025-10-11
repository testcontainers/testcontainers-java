package org.testcontainers.junit.oracle;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.utility.DockerImageName;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SimpleOracleTest extends AbstractContainerDatabaseTest {

    public static final DockerImageName ORACLE_DOCKER_IMAGE_NAME = DockerImageName.parse(
        "gvenzl/oracle-xe:21-slim-faststart"
    );

    private void runTest(OracleContainer container, String databaseName, String username, String password)
        throws SQLException {
        //Test config was honored
        assertThat(container.getDatabaseName()).isEqualTo(databaseName);
        assertThat(container.getUsername()).isEqualTo(username);
        assertThat(container.getPassword()).isEqualTo(password);

        //Test we can get a connection
        container.start();
        performQuery(
            container,
            "SELECT 1 FROM dual",
            resultSet -> {
                assertThatNoException()
                    .isThrownBy(() -> {
                        int resultSetInt = resultSet.getInt(1);
                        assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
                    });
            }
        );
    }

    @Test
    void testDefaultSettings() throws SQLException {
        try ( // container {
            OracleContainer oracle = new OracleContainer("gvenzl/oracle-xe:21-slim-faststart")
            // }
        ) {
            runTest(oracle, "xepdb1", "test", "test");

            // Match against the last '/'
            String urlSuffix = oracle.getJdbcUrl().split("(\\/)(?!.*\\/)", 2)[1];
            assertThat(urlSuffix).isEqualTo("xepdb1");
        }
    }

    @Test
    void testPluggableDatabase() throws SQLException {
        try (OracleContainer oracle = new OracleContainer(ORACLE_DOCKER_IMAGE_NAME).withDatabaseName("testDB")) {
            runTest(oracle, "testDB", "test", "test");
        }
    }

    @Test
    void testPluggableDatabaseAndCustomUser() throws SQLException {
        try (
            OracleContainer oracle = new OracleContainer("gvenzl/oracle-xe:21-slim-faststart")
                .withDatabaseName("testDB")
                .withUsername("testUser")
                .withPassword("testPassword")
        ) {
            runTest(oracle, "testDB", "testUser", "testPassword");
        }
    }

    @Test
    void testCustomUser() throws SQLException {
        try (
            OracleContainer oracle = new OracleContainer(ORACLE_DOCKER_IMAGE_NAME)
                .withUsername("testUser")
                .withPassword("testPassword")
        ) {
            runTest(oracle, "xepdb1", "testUser", "testPassword");
        }
    }

    @Test
    void testSID() throws SQLException {
        try (OracleContainer oracle = new OracleContainer(ORACLE_DOCKER_IMAGE_NAME).usingSid()) {
            runTest(oracle, "xepdb1", "system", "test");

            // Match against the last ':'
            String urlSuffix = oracle.getJdbcUrl().split("(\\:)(?!.*\\:)", 2)[1];
            assertThat(urlSuffix).isEqualTo("xe");
        }
    }

    @Test
    void testSIDAndCustomPassword() throws SQLException {
        try (
            OracleContainer oracle = new OracleContainer(ORACLE_DOCKER_IMAGE_NAME)
                .usingSid()
                .withPassword("testPassword")
        ) {
            runTest(oracle, "xepdb1", "system", "testPassword");
        }
    }

    @Test
    void testErrorPaths() {
        try (OracleContainer oracle = new OracleContainer(ORACLE_DOCKER_IMAGE_NAME)) {
            assertThatThrownBy(() -> oracle.withDatabaseName("XEPDB1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Should not have been able to set database name to XEPDB1.");

            assertThatThrownBy(() -> oracle.withDatabaseName(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Should not have been able to set database name to nothing.");

            assertThatThrownBy(() -> oracle.withUsername("SYSTEM"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Should not have been able to set username to system.");

            assertThatThrownBy(() -> oracle.withUsername("SYS"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Should not have been able to set username to sys.");

            assertThatThrownBy(() -> oracle.withUsername(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Should not have been able to set username to nothing.");

            assertThatThrownBy(() -> oracle.withPassword(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Should not have been able to set password to nothing.");
        }
    }
}
