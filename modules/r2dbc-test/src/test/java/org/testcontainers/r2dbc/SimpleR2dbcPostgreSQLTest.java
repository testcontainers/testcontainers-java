package org.testcontainers.r2dbc;

import io.r2dbc.client.R2dbc;
import org.junit.Test;
import org.testcontainers.r2dbc.R2dbcPostgresContainer;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;

public class SimpleR2dbcPostgreSQLTest {

    @Test
    public void testR2dbcConnectionSuccessful() {
        try (R2dbcPostgresContainer postgres = new R2dbcPostgresContainer()
            .withInitScript("init_postgresql.sql")) {
            postgres.start();

            R2dbc r2dbc = new R2dbc(postgres.getR2dbcConnectionFactory());
            String queryResult = (String) r2dbc.inTransaction(handle ->
                handle.createQuery("SELECT foo FROM bar").mapResult(result -> result.map((row, metadata) -> row.get("foo")))
            ).blockFirst();
            assertEquals("Value from init script should equal real value", "hello world", queryResult);
        }
    }
}
