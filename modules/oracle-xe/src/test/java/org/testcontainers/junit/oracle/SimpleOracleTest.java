package org.testcontainers.junit.oracle;

import org.junit.Ignore;
import org.junit.Test;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

@Ignore
public class SimpleOracleTest extends AbstractContainerDatabaseTest {

    @Test
    public void testSimple() throws SQLException {
        try (OracleContainer oracle = new OracleContainer()) {
            ResultSet resultSet = performQuery(oracle, "SELECT 1 FROM dual");

            int resultSetInt = resultSet.getInt(1);

            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
        }
    }
}
