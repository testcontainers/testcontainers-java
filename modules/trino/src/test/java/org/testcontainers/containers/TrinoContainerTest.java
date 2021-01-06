package org.testcontainers.containers;

import org.junit.Assert;
import org.junit.Test;
import org.testcontainers.TrinoTestImages;

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

public class TrinoContainerTest {

    @Test
    public void testSimple() throws Exception {
        try (TrinoContainer trino = new TrinoContainer(TrinoTestImages.TRINO_TEST_IMAGE)) {
            trino.start();
            try (Connection connection = trino.createConnection();
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT DISTINCT node_version FROM system.runtime.nodes")) {
                assertTrue("No result", resultSet.next());
                assertEquals("Trino version", TrinoContainer.DEFAULT_TAG, resultSet.getString("node_version"));
            }
        }
    }

    @Test
    public void testSpecificVersion() throws Exception {
        try (TrinoContainer trino = new TrinoContainer(TrinoTestImages.TRINO_PREVIOUS_VERSION_TEST_IMAGE)) {
            trino.start();
            try (Connection connection = trino.createConnection();
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT DISTINCT node_version FROM system.runtime.nodes")) {
                assertTrue("No result", resultSet.next());
                assertEquals("Trino version", TrinoTestImages.TRINO_PREVIOUS_VERSION_TEST_IMAGE.getVersionPart(), resultSet.getString("node_version"));
            }
        }
    }

    @Test
    public void testInitScript() throws Exception {
        try (TrinoContainer trino = new TrinoContainer(TrinoTestImages.TRINO_TEST_IMAGE)) {
            trino.withInitScript("initial.sql");
            trino.start();
            try (Connection connection = trino.createConnection();
                 Statement statement = connection.createStatement();
                 ResultSet resultSet = statement.executeQuery("SELECT a FROM memory.default.test_table")) {
                assertTrue("No result", resultSet.next());
                assertEquals("Value", 12345678909324L, resultSet.getObject("a"));
                assertFalse("Too many result", resultSet.next());
            }
        }
    }
}
