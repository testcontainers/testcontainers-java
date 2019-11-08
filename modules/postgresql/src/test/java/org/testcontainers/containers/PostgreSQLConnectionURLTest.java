package org.testcontainers.containers;

import org.junit.Test;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertFalse;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

public class PostgreSQLConnectionURLTest {

    @Test
    public void shouldCorrectlyAppendQueryString() {
        PostgreSQLContainer postgres = new FixedJdbcUrlPostgreSQLContainer();
        String connectionUrl = postgres.constructUrlForConnection("?stringtype=unspecified&stringtype=unspecified");
        String queryString = connectionUrl.substring(connectionUrl.indexOf('?'));

        assertTrue("Query String contains expected params", queryString.contains("&stringtype=unspecified&stringtype=unspecified"));
        assertEquals("Query String starts with '?'", 0, queryString.indexOf('?'));
        assertFalse("Query String does not contain extra '?'", queryString.substring(1).contains("?"));
    }

    public static class FixedJdbcUrlPostgreSQLContainer extends PostgreSQLContainer {

        @Override
        public String getJdbcUrl() {
            return "jdbc:postgresql://localhost:34532/databasename?loggerLevel=OFF";
        }
    }
}
