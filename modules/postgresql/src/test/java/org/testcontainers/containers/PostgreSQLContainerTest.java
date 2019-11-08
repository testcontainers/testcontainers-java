package org.testcontainers.containers;

import org.junit.Rule;
import org.junit.Test;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertFalse;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;
import static org.testcontainers.jdbc.ConnectionUrl.newInstance;

public class PostgreSQLContainerTest {

    @Rule
    public JdbcDatabaseContainer postgres = new PostgreSQLContainerProvider().newInstance(newInstance("jdbc:tc:postgresql://hostname/databasename"));

    @Test
    public void shouldCorrectlyAppendQueryString() {
        String connectionUrl = postgres.constructUrlForConnection("?stringtype=unspecified&stringtype=unspecified");
        String queryString = connectionUrl.substring(connectionUrl.indexOf('?'));

        assertTrue("Query String contains expected params", queryString.contains("&stringtype=unspecified&stringtype=unspecified"));
        assertEquals("Query String starts with '?'", 0, queryString.indexOf('?'));
        assertFalse("Query String does not contain extra '?'", queryString.substring(1).contains("?"));
    }
}
