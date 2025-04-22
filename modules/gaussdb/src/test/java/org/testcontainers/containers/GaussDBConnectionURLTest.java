package org.testcontainers.containers;

import org.junit.Test;
import org.testcontainers.GaussDBTestImages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class GaussDBConnectionURLTest {

    @Test
    public void shouldCorrectlyAppendQueryString() {
        GaussDBContainer<?> gaussDB = new FixedJdbcUrlGaussDBContainer();
        String connectionUrl = gaussDB.constructUrlForConnection("?stringtype=unspecified&stringtype=unspecified");
        String queryString = connectionUrl.substring(connectionUrl.indexOf('?'));

        assertThat(queryString)
            .as("Query String contains expected params")
            .contains("?stringtype=unspecified&stringtype=unspecified");
        assertThat(queryString.indexOf('?')).as("Query String starts with '?'").isZero();
        assertThat(queryString.substring(1)).as("Query String does not contain extra '?'").doesNotContain("?");
    }

    @Test
    public void shouldCorrectlyAppendQueryStringWhenNoBaseParams() {
        GaussDBContainer<?> gaussDB = new NoParamsUrlGaussDBContainer();
        String connectionUrl = gaussDB.constructUrlForConnection("?stringtype=unspecified&stringtype=unspecified");
        String queryString = connectionUrl.substring(connectionUrl.indexOf('?'));

        assertThat(queryString)
            .as("Query String contains expected params")
            .contains("?stringtype=unspecified&stringtype=unspecified");
        assertThat(queryString.indexOf('?')).as("Query String starts with '?'").isZero();
        assertThat(queryString.substring(1)).as("Query String does not contain extra '?'").doesNotContain("?");
    }

    @Test
    public void shouldReturnOriginalURLWhenEmptyQueryString() {
        GaussDBContainer<?> gaussDB = new FixedJdbcUrlGaussDBContainer();
        String connectionUrl = gaussDB.constructUrlForConnection("");

        assertThat(gaussDB.getJdbcUrl()).as("Query String remains unchanged").isEqualTo(connectionUrl);
    }

    @Test
    public void shouldRejectInvalidQueryString() {
        assertThat(
            catchThrowable(() -> {
                new NoParamsUrlGaussDBContainer().constructUrlForConnection("stringtype=unspecified");
            })
        )
            .as("Fails when invalid query string provided")
            .isInstanceOf(IllegalArgumentException.class);
    }

    static class FixedJdbcUrlGaussDBContainer extends GaussDBContainer<FixedJdbcUrlGaussDBContainer> {

        public FixedJdbcUrlGaussDBContainer() {
            super(GaussDBTestImages.GAUSSDB_TEST_IMAGE);
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

    static class NoParamsUrlGaussDBContainer extends GaussDBContainer<FixedJdbcUrlGaussDBContainer> {

        public NoParamsUrlGaussDBContainer() {
            super(GaussDBTestImages.GAUSSDB_TEST_IMAGE);
        }

        @Override
        public String getJdbcUrl() {
            return "jdbc:gaussdb://host:port/database";
        }
    }
}
