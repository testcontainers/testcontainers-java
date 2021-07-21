package org.testcontainers.junit.oracle;

import org.junit.Test;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

public class SimpleOracleTest extends AbstractContainerDatabaseTest {

    @Test
    public void testSimple() throws SQLException {

        // Oracle Express Edition 11g Release 2 on Ubuntu 18.04 LTS
        String oracleImage = "oracleinanutshell/oracle-xe-11g:1.0.0";

        try (OracleContainer oracle = new OracleContainer(oracleImage)) {
            oracle.start();
            ResultSet resultSet = performQuery(oracle, "SELECT 1 FROM dual");

            int resultSetInt = resultSet.getInt(1);

            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
        }
    }
}
