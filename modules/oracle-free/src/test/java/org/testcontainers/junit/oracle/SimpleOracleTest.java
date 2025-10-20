package org.testcontainers.junit.oracle;

import org.junit.jupiter.api.Test;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.oracle.OracleContainer;
import org.testcontainers.utility.DockerImageName;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

class SimpleOracleTest extends AbstractContainerDatabaseTest {

    private static final DockerImageName ORACLE_DOCKER_IMAGE_NAME = DockerImageName.parse(
        "gvenzl/oracle-free:slim-faststart"
    );

    private void runTest(OracleContainer container, String databaseName, String username, String password)
        throws SQLException {
        //Test config was honored
        assertThat(container.getDatabaseName()).isEqualTo(databaseName);
        assertThat(container.getUsername()).isEqualTo(username);
        assertThat(container.getPassword()).isEqualTo(password);

        //Test we can get a connection
        container.start();
        ResultSet resultSet = performQuery(container, "SELECT 1 FROM dual");
        int resultSetInt = resultSet.getInt(1);
        assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
    }

    @Test
    void testDefaultSettings() throws SQLException {
        try ( // container {
            OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:slim-faststart")
            // }
        ) {
            runTest(oracle, "freepdb1", "test", "test");

            // Match against the last '/'
            String urlSuffix = oracle.getJdbcUrl().split("(\\/)(?!.*\\/)", 2)[1];
            assertThat(urlSuffix).isEqualTo("freepdb1");
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
            OracleContainer oracle = new OracleContainer("gvenzl/oracle-free:slim-faststart")
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
            runTest(oracle, "freepdb1", "testUser", "testPassword");
        }
    }

    @Test
    void testSID() throws SQLException {
        try (OracleContainer oracle = new OracleContainer(ORACLE_DOCKER_IMAGE_NAME).usingSid()) {
            runTest(oracle, "freepdb1", "system", "test");

            // Match against the last ':'
            String urlSuffix = oracle.getJdbcUrl().split("(\\:)(?!.*\\:)", 2)[1];
            assertThat(urlSuffix).isEqualTo("free");
        }
    }

    @Test
    void testSIDAndCustomPassword() throws SQLException {
        try (
            OracleContainer oracle = new OracleContainer(ORACLE_DOCKER_IMAGE_NAME)
                .usingSid()
                .withPassword("testPassword")
        ) {
            runTest(oracle, "freepdb1", "system", "testPassword");
        }
    }

    @Test
    void testErrorPaths() throws SQLException {
        try (OracleContainer oracle = new OracleContainer(ORACLE_DOCKER_IMAGE_NAME)) {
            try {
                oracle.withDatabaseName("FREEPDB1");
                fail("Should not have been able to set database name to freepdb1.");
            } catch (IllegalArgumentException e) {
                //expected
            }

            try {
                oracle.withDatabaseName("");
                fail("Should not have been able to set database name to nothing.");
            } catch (IllegalArgumentException e) {
                //expected
            }

            try {
                oracle.withUsername("SYSTEM");
                fail("Should not have been able to set username to system.");
            } catch (IllegalArgumentException e) {
                //expected
            }

            try {
                oracle.withUsername("SYS");
                fail("Should not have been able to set username to sys.");
            } catch (IllegalArgumentException e) {
                //expected
            }

            try {
                oracle.withUsername("");
                fail("Should not have been able to set username to nothing.");
            } catch (IllegalArgumentException e) {
                //expected
            }

            try {
                oracle.withPassword("");
                fail("Should not have been able to set password to nothing.");
            } catch (IllegalArgumentException e) {
                //expected
            }
        }
    }
}
