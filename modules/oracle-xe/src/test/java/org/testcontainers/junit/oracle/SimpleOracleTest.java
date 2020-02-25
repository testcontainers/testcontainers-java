package org.testcontainers.junit.oracle;

import org.junit.Ignore;
import org.junit.Test;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

public class SimpleOracleTest extends AbstractContainerDatabaseTest {

    @Test
    public void testSimple() throws SQLException {
        try (OracleContainer oracle = new OracleContainer("kyleaure/oracle-18.4.0-xe-prebuilt:1.0")) {
            oracle.start();

            ResultSet resultSet = performQuery(oracle, "SELECT 1 FROM dual");

            int resultSetInt = resultSet.getInt(1);

            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
        }
    }

    @Test
    public void testSimpleServiceName() throws SQLException {
        try (OracleContainer oracle = new OracleContainer("kyleaure/oracle-18.4.0-xe-prebuilt:1.0")){
            oracle.withServiceName("XEPDB1");
            oracle.start();

            ResultSet resultSet = performQuery(oracle, "SELECT 1 FROM dual");

            int resultSetInt = resultSet.getInt(1);

            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
        }
    }
}
