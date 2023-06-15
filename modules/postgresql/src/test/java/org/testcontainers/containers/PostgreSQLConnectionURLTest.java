package org.testcontainers.containers;

import org.junit.Test;
import org.testcontainers.PostgreSQLTestImages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class PostgreSQLConnectionURLTest {

    @Test
    public void shouldCorrectlyAppendQueryString() {
        PostgreSQLContainer<?> postgres = new FixedJdbcUrlPostgreSQLContainer();
        String connectionUrl = postgres.constructUrlForConnection("?stringtype=unspecified&stringtype=unspecified");
        String queryString = connectionUrl.substring(connectionUrl.indexOf('?'));

        assertThat(queryString)
            .as("Query String contains expected params")
            .contains("?stringtype=unspecified&stringtype=unspecified");
        assertThat(queryString.indexOf('?')).as("Query String starts with '?'").isZero();
        assertThat(queryString.substring(1)).as("Query String does not contain extra '?'").doesNotContain("?");
    }

    @Test
    public void shouldCorrectlyAppendQueryStringWhenNoBaseParams() {
        PostgreSQLContainer<?> postgres = new NoParamsUrlPostgreSQLContainer();
        String connectionUrl = postgres.constructUrlForConnection("?stringtype=unspecified&stringtype=unspecified");
        String queryString = connectionUrl.substring(connectionUrl.indexOf('?'));

        assertThat(queryString)
            .as("Query String contains expected params")
            .contains("?stringtype=unspecified&stringtype=unspecified");
        assertThat(queryString.indexOf('?')).as("Query String starts with '?'").isZero();
        assertThat(queryString.substring(1)).as("Query String does not contain extra '?'").doesNotContain("?");
    }

    @Test
    public void shouldReturnOriginalURLWhenEmptyQueryString() {
        PostgreSQLContainer<?> postgres = new FixedJdbcUrlPostgreSQLContainer();
        String connectionUrl = postgres.constructUrlForConnection("");

        assertThat(postgres.getJdbcUrl()).as("Query String remains unchanged").isEqualTo(connectionUrl);
    }

    @Test
    public void shouldRejectInvalidQueryString() {
        assertThat(
            catchThrowable(() -> {
                new NoParamsUrlPostgreSQLContainer().constructUrlForConnection("stringtype=unspecified");
            })
        )
            .as("Fails when invalid query string provided")
            .isInstanceOf(IllegalArgumentException.class);
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
