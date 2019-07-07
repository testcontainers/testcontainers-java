package org.testcontainers.junit;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.OracleContainer;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

/**
 * @author gusohal
 */
@Ignore
public class SimpleOracleTest extends AbstractContainerDatabaseTest {

    @Rule
    public OracleContainer oracle = new OracleContainer();

    @Test
    public void testSimple() throws SQLException {
        ResultSet resultSet = performQuery(oracle, "SELECT 1 FROM dual");

        resultSet.next();
        int resultSetInt = resultSet.getInt(1);

        assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
    }
}
