package org.testcontainers.containers;

import org.junit.Test;
import org.testcontainers.CrateDBTestImages;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

public class CrateDBLegacyDriverConnectionURLTest {

    @Test
    public void shouldCorrectlyAppendQueryString() {
        CrateDBContainer<?> cratedb = new FixedJdbcUrlCrateDBContainer();
        String connectionUrl = cratedb.constructUrlForConnection("?stringtype=unspecified&stringtype=unspecified");
        String queryString = connectionUrl.substring(connectionUrl.indexOf('?'));

        assertThat(queryString)
            .as("Query String contains expected params")
            .contains("?stringtype=unspecified&stringtype=unspecified");
        assertThat(queryString.indexOf('?')).as("Query String starts with '?'").isZero();
        assertThat(queryString.substring(1)).as("Query String does not contain extra '?'").doesNotContain("?");
    }

    @Test
    public void shouldCorrectlyAppendQueryStringWhenNoBaseParams() {
        CrateDBContainer<?> cratedb = new NoParamsUrlCrateDBContainer();
        String connectionUrl = cratedb.constructUrlForConnection("?stringtype=unspecified&stringtype=unspecified");
        String queryString = connectionUrl.substring(connectionUrl.indexOf('?'));

        assertThat(queryString)
            .as("Query String contains expected params")
            .contains("?stringtype=unspecified&stringtype=unspecified");
        assertThat(queryString.indexOf('?')).as("Query String starts with '?'").isZero();
        assertThat(queryString.substring(1)).as("Query String does not contain extra '?'").doesNotContain("?");
    }

    @Test
    public void shouldReturnOriginalURLWhenEmptyQueryString() {
        CrateDBContainer<?> cratedb = new FixedJdbcUrlCrateDBContainer();
        String connectionUrl = cratedb.constructUrlForConnection("");

        assertThat(cratedb.getJdbcUrl()).as("Query String remains unchanged").isEqualTo(connectionUrl);
    }

    @Test
    public void shouldRejectInvalidQueryString() {
        assertThat(
            catchThrowable(() -> {
                new NoParamsUrlCrateDBContainer().constructUrlForConnection("stringtype=unspecified");
            })
        )
            .as("Fails when invalid query string provided")
            .isInstanceOf(IllegalArgumentException.class);
    }

    static class FixedJdbcUrlCrateDBContainer extends CrateDBLegacyDriverContainer<FixedJdbcUrlCrateDBContainer> {

        public FixedJdbcUrlCrateDBContainer() {
            super(CrateDBTestImages.CRATEDB_TEST_IMAGE);
        }

        @Override
        public String getHost() {
            return "localhost";
        }

        @Override
        public Integer getMappedPort(int originalPort) {
            return 5432;
        }
    }

    static class NoParamsUrlCrateDBContainer extends CrateDBLegacyDriverContainer<FixedJdbcUrlCrateDBContainer> {

        public NoParamsUrlCrateDBContainer() {
            super(CrateDBTestImages.CRATEDB_TEST_IMAGE);
        }

        @Override
        public String getJdbcUrl() {
            return "jdbc:crate://host:port/database";
        }
    }
}
