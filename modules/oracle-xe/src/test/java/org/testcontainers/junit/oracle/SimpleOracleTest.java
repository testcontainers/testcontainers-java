package org.testcontainers.junit.oracle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.utility.DockerImageName;

import java.sql.ResultSet;
import java.sql.SQLException;

public class SimpleOracleTest extends AbstractContainerDatabaseTest {

    public static final DockerImageName ORACLE_DOCKER_IMAGE_NAME = DockerImageName.parse("gvenzl/oracle-xe:18.4.0-slim");

    private void runTest(OracleContainer container, String databaseName, String username, String password) throws SQLException {
        //Test config was honored
        assertEquals(container.getDatabaseName(), databaseName);
        assertEquals(container.getUsername(), username);
        assertEquals(container.getPassword(), password);
        
        //Test we can get a connection
        container.start();
        ResultSet resultSet = performQuery(container, "SELECT 1 FROM dual");
        int resultSetInt = resultSet.getInt(1);
        assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
    }

    @Test
    public void testDefaultSettings() throws SQLException {
        try (
            OracleContainer oracle = new OracleContainer(ORACLE_DOCKER_IMAGE_NAME);
        ) {
            assertFalse(oracle.isUsingSid());
            runTest(oracle, "xepdb1", "test", "test");
        }
    }

    @Test
    public void testPluggableDatabase() throws SQLException {
        try (
            OracleContainer oracle = new OracleContainer(ORACLE_DOCKER_IMAGE_NAME)
                .withDatabaseName("testDB")
        ) {
            runTest(oracle, "testDB", "test", "test");
        }
    }

    @Test
    public void testPluggableDatabaseAndCustomUser() throws SQLException {
        try (
            OracleContainer oracle = new OracleContainer(ORACLE_DOCKER_IMAGE_NAME)
                .withDatabaseName("testDB")
                .withUsername("testUser")
                .withPassword("testPassword")
        ) {
            runTest(oracle, "testDB", "testUser", "testPassword");
        }
    }

    @Test
    public void testCustomUser() throws SQLException {
        try (
            OracleContainer oracle = new OracleContainer(ORACLE_DOCKER_IMAGE_NAME)
                .withUsername("testUser")
                .withPassword("testPassword")
        ) {
            runTest(oracle, "xepdb1", "testUser", "testPassword");
        }
    }

    @Test
    public void testSID() throws SQLException {
        try (
            OracleContainer oracle = new OracleContainer(ORACLE_DOCKER_IMAGE_NAME)
                .usingSid();
        ) {
            assertTrue(oracle.isUsingSid());
            runTest(oracle, "xepdb1", "system", "test");
        }
    }

    @Test
    public void testSIDAndCustomPassword() throws SQLException {
        try (
            OracleContainer oracle = new OracleContainer(ORACLE_DOCKER_IMAGE_NAME)
                .usingSid()
                .withPassword("testPassword");
        ) {
            assertTrue(oracle.isUsingSid());
            runTest(oracle, "xepdb1", "system", "testPassword");
        }
    }
}
