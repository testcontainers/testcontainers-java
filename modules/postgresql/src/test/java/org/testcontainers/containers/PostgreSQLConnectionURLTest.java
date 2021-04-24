package org.testcontainers.containers;

import org.junit.Test;
import org.testcontainers.PostgreSQLTestImages;

import static org.rnorth.visibleassertions.VisibleAssertions.assertEquals;
import static org.rnorth.visibleassertions.VisibleAssertions.assertFalse;
import static org.rnorth.visibleassertions.VisibleAssertions.assertThrows;
import static org.rnorth.visibleassertions.VisibleAssertions.assertTrue;

public class PostgreSQLConnectionURLTest {

    @Test
    public void shouldCorrectlyAppendQueryString() {
        PostgreSQLContainer<?> postgres = new FixedJdbcUrlPostgreSQLContainer();
        String connectionUrl = postgres.constructUrlForConnection("?stringtype=unspecified&stringtype=unspecified");
        String queryString = connectionUrl.substring(connectionUrl.indexOf('?'));

        assertTrue("Query String contains expected params", queryString.contains("?stringtype=unspecified&stringtype=unspecified"));
        assertEquals("Query String starts with '?'", 0, queryString.indexOf('?'));
        assertFalse("Query String does not contain extra '?'", queryString.substring(1).contains("?"));
    }

    @Test
    public void shouldCorrectlyAppendQueryStringWhenNoBaseParams() {
        PostgreSQLContainer<?> postgres = new NoParamsUrlPostgreSQLContainer();
        String connectionUrl = postgres.constructUrlForConnection("?stringtype=unspecified&stringtype=unspecified");
        String queryString = connectionUrl.substring(connectionUrl.indexOf('?'));

        assertTrue("Query String contains expected params", queryString.contains("?stringtype=unspecified&stringtype=unspecified"));
        assertEquals("Query String starts with '?'", 0, queryString.indexOf('?'));
        assertFalse("Query String does not contain extra '?'", queryString.substring(1).contains("?"));
    }

    @Test
    public void shouldReturnOriginalURLWhenEmptyQueryString() {
        PostgreSQLContainer<?> postgres = new FixedJdbcUrlPostgreSQLContainer();
        String connectionUrl = postgres.constructUrlForConnection("");

        assertTrue("Query String remains unchanged", postgres.getJdbcUrl().equals(connectionUrl));
    }

    @Test
    public void shouldRejectInvalidQueryString() {
        assertThrows("Fails when invalid query string provided", IllegalArgumentException.class,
            () -> new NoParamsUrlPostgreSQLContainer().constructUrlForConnection("stringtype=unspecified"));
    }

    static class FixedJdbcUrlPostgreSQLContainer extends PostgreSQLContainer<FixedJdbcUrlPostgreSQLContainer> {
        public FixedJdbcUrlPostgreSQLContainer() {
            super(PostgreSQLTestImages.POSTGRES_TEST_IMAGE);
        }

        @Override
        public String getHost() {
            return "localhost";
        }

        @Override
        public Integer getMappedPort(int originalPort) {
            return 34532;
        }
    }

    static class NoParamsUrlPostgreSQLContainer extends PostgreSQLContainer<FixedJdbcUrlPostgreSQLContainer> {
        public NoParamsUrlPostgreSQLContainer() {
            super(PostgreSQLTestImages.POSTGRES_TEST_IMAGE);
        }

        @Override
        public String getJdbcUrl() {
            return "jdbc:postgresql://host:port/database";
        }
    }
}
