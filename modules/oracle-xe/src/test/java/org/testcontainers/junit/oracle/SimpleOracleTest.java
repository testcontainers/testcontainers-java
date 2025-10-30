package org.testcontainers.junit.oracle;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.utility.DockerImageName;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

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
        ResultSet resultSet = performQuery(container, "SELECT 1 FROM dual");
        int resultSetInt = resultSet.getInt(1);
        assertThat(resultSetInt).as("A basic SELECT query succeeds").isEqualTo(1);
    }

    private void runTestSystemUser(OracleContainer container, String databaseName, String username, String password)
        throws SQLException {
        assertThat(container.getDatabaseName()).isEqualTo(databaseName);
        assertThat(container.getUsername()).isEqualTo(username);
        assertThat(container.getPassword()).isEqualTo(password);

        container.start();
        ResultSet resultSet = performQuery(container, "GRANT DBA TO " + username);
        int resultSetInt = resultSet.getInt(1);
        assertThat(resultSetInt).as("A basic system user query succeeds").isEqualTo(1);
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
        try (OracleContainer oracle = new OracleContainer(ORACLE_DOCKER_IMAGE_NAME).usingSid();) {
            runTestSystemUser(oracle, "xepdb1", "system", "test");

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
                .withPassword("testPassword");
        ) {
            runTestSystemUser(oracle, "xepdb1", "system", "testPassword");
        }
    }

    @Test
    public void testSeparateSystemAndAppPasswords() throws SQLException {
        try (
            OracleContainer oracleSid = new OracleContainer(ORACLE_DOCKER_IMAGE_NAME)
                .usingSid()
                .withSystemPassword("SysP@ss1!")
                .withPassword("AppP@ss1!")
        ) {
            runTestSystemUser(oracleSid, "xepdb1", "system", "SysP@ss1!");
        }

        // Non-SID mode should use application user's password
        try (
            OracleContainer oraclePdb = new OracleContainer(ORACLE_DOCKER_IMAGE_NAME)
                .withSystemPassword("SysP@ss2!")
                .withPassword("AppP@ss2!")
        ) {
            runTest(oraclePdb, "xepdb1", "test", "AppP@ss2!");
        }
    }

    @Test
    void testErrorPaths() throws SQLException {
        try (OracleContainer oracle = new OracleContainer(ORACLE_DOCKER_IMAGE_NAME)) {
            try {
                oracle.withDatabaseName("XEPDB1");
                fail("Should not have been able to set database name to xepdb1.");
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
