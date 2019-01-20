package org.testcontainers.r2dbc;

import io.r2dbc.client.R2dbc;
import org.junit.Rule;
import org.junit.Test;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

public class SimpleR2dbcMSSQLServerTest {

    @Rule
    public R2dbcMSSQLServerContainer mssqlServer = new R2dbcMSSQLServerContainer();

    @Test
    public void testR2dbcConnectionSuccessful() {
        R2dbc r2dbc = new R2dbc(mssqlServer.getR2dbcConnectionFactory());
        Object resultSetInt =  r2dbc.inTransaction(handle -> handle.createQuery("SELECT 1").mapRow(row -> row.get(""))).blockFirst();
        assertEquals("A basic SELECT query succeeds", 1, resultSetInt);
    }
}
