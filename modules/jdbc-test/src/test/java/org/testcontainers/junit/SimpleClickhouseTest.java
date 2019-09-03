package org.testcontainers.junit;

import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.containers.ClickHouseContainer;

import java.sql.ResultSet;
import java.sql.SQLException;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

public class SimpleClickhouseTest extends AbstractContainerDatabaseTest {

    @Rule
    public ClickHouseContainer clickhouse = new ClickHouseContainer();

    @Test
    public void testSimple() throws SQLException {
        ResultSet resultSet = performQuery(clickhouse, "SELECT 1");

        resultSet.next();
        int resultSetInt = resultSet.getInt(1);
        assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
    }
}
