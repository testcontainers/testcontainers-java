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

        String oracleImage = "gvenzl/oracle-xe";

        try (
            OracleContainer oracle = new OracleContainer(oracleImage)
                .withPassword("foobar")
                .withEnv("ORACLE_PASSWORD", "foobar")
        ) {
            oracle.start();
            ResultSet resultSet = performQuery(oracle, "SELECT 1 FROM dual");

            int resultSetInt = resultSet.getInt(1);

            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
        }
    }
}
