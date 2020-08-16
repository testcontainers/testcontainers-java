package org.testcontainers.containers;

import org.junit.Assert;
import org.junit.Test;
import org.testcontainers.PrestoTestImages;
import org.testcontainers.utility.DockerImageName;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * @author findepi
 */
public class PrestoContainerTest {

    @Test
    public void testSimple() throws Exception {
        try (PrestoContainer<?> prestoSql = new PrestoContainer<>(PrestoTestImages.PRESTO_TEST_IMAGE)) {
            prestoSql.start();
            try (Connection connection = prestoSql.createConnection();
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT DISTINCT node_version FROM system.runtime.nodes")) {
                assertTrue("No result", resultSet.next());
                assertEquals("Presto version", PrestoContainer.DEFAULT_TAG, resultSet.getString("node_version"));
            }
        }
    }

    @Test
    public void testSpecificVersion() throws Exception {
        try (PrestoContainer<?> prestoSql = new PrestoContainer<>(PrestoTestImages.PRESTO_PREVIOUS_VERSION_TEST_IMAGE)) {
            prestoSql.start();
            try (Connection connection = prestoSql.createConnection();
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT DISTINCT node_version FROM system.runtime.nodes")) {
                assertTrue("No result", resultSet.next());
                assertEquals("Presto version", PrestoTestImages.PRESTO_PREVIOUS_VERSION_TEST_IMAGE.getVersionPart(), resultSet.getString("node_version"));
            }
        }
    }

    @Test
    public void testQueryMemoryAndTpch() throws SQLException {
        try (PrestoContainer<?> prestoSql = new PrestoContainer<>(PrestoTestImages.PRESTO_TEST_IMAGE)) {
            prestoSql.start();
            try (Connection connection = prestoSql.createConnection();
                 Statement statement = connection.createStatement()) {
                // Prepare data
                statement.execute("CREATE TABLE memory.default.table_with_array AS SELECT 1 id, ARRAY[1, 42, 2, 42, 4, 42] my_array");

                // Query Presto using newly created table and a builtin connector
                try (ResultSet resultSet = statement.executeQuery("" +
                    "SELECT nationkey, element " +
                    "FROM tpch.tiny.nation " +
                    "JOIN memory.default.table_with_array twa ON nationkey = twa.id " +
                    "LEFT JOIN UNNEST(my_array) a(element) ON true " +
                    "ORDER BY element OFFSET 1 FETCH NEXT 3 ROWS WITH TIES ")) {
                    List<Integer> actualElements = new ArrayList<>();
                    while (resultSet.next()) {
                        actualElements.add(resultSet.getInt("element"));
                    }
                    Assert.assertEquals(Arrays.asList(2, 4, 42, 42, 42), actualElements);
                }
            }
        }
    }

    @Test
    public void testInitScript() throws Exception {
        try (PrestoContainer<?> prestoSql = new PrestoContainer<>(PrestoTestImages.PRESTO_TEST_IMAGE)) {
            prestoSql.withInitScript("initial.sql");
            prestoSql.start();
            try (Connection connection = prestoSql.createConnection();
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT a FROM memory.default.test_table")) {
                assertTrue("No result", resultSet.next());
                assertEquals("Value", 12345678909324L, resultSet.getObject("a"));
                assertFalse("Too many result", resultSet.next());
            }
        }
    }

    @Test
    public void testTcJdbcUri() throws Exception {
        try (Connection connection = DriverManager.getConnection(format("jdbc:tc:presto:%s://hostname/", PrestoContainer.DEFAULT_TAG))) {
            // Verify metadata with tc: JDBC connection URI
            assertEquals(connection.getMetaData().getDatabaseMajorVersion(), parseInt(PrestoContainer.DEFAULT_TAG));

            // Verify transactions with tc: JDBC connection URI
            assertTrue("Is autocommit", connection.getAutoCommit());
            connection.setAutoCommit(false);
            assertFalse("Is autocommit", connection.getAutoCommit());
            assertEquals("Transaction isolation", Connection.TRANSACTION_READ_UNCOMMITTED, connection.getTransactionIsolation());

            try (Statement statement = connection.createStatement()) {
                assertEquals("Update result", 0, statement.executeUpdate("CREATE TABLE memory.default.test_tc(a bigint)"));
                try (ResultSet resultSet = statement.executeQuery("SELECT sum(cast(node_version AS bigint)) AS v FROM system.runtime.nodes")) {
                    assertTrue(resultSet.next());
                    assertEquals(PrestoContainer.DEFAULT_TAG, resultSet.getString("v"));
                    assertFalse(resultSet.next());
                }
                connection.commit();
            } finally {
                connection.rollback();
            }
            connection.setAutoCommit(true);
            assertTrue("Is autocommit", connection.getAutoCommit());
            assertEquals("Transaction isolation should be retained", Connection.TRANSACTION_READ_UNCOMMITTED, connection.getTransactionIsolation());
        }
    }
}
