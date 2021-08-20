package org.testcontainers.junit.oracle;

import org.junit.Test;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.db.AbstractContainerDatabaseTest;
import org.testcontainers.utility.DockerImageName;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

public class SimpleOracleTest extends AbstractContainerDatabaseTest {

    @Test
    public void testSimple() throws SQLException {

        try (
            // constructor {
            OracleContainer oracle = new OracleContainer(DockerImageName.parse("gvenzl/oracle-xe:18.4.0-slim"))
                .withUsername("baz")
                .withPassword("bar")
            // }
        ) {
            oracle.start();
            ResultSet resultSet = performQuery(oracle, "SELECT 1 FROM dual");

            int resultSetInt = resultSet.getInt(1);

            assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
        }
    }
}
