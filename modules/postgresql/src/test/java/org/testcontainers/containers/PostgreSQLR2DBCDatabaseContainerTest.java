package org.testcontainers.containers;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.junit.Test;
import org.testcontainers.PostgreSQLTestImages;
import org.testcontainers.r2dbc.AbstractR2DBCDatabaseContainerTest;

import static org.assertj.core.api.Assertions.assertThat;

public class PostgreSQLR2DBCDatabaseContainerTest extends AbstractR2DBCDatabaseContainerTest<PostgreSQLContainer<?>> {

    @Override
    protected PostgreSQLContainer<?> createContainer() {
        return new PostgreSQLContainer<>(PostgreSQLTestImages.POSTGRES_TEST_IMAGE);
    }

    @Override
    protected ConnectionFactoryOptions getOptions(PostgreSQLContainer<?> container) {
        // spotless:off
        // get_options {
        ConnectionFactoryOptions options = PostgreSQLR2DBCDatabaseContainer.getOptions(
            container
        );
        // }
        // spotless:on

        return options;
    }

    protected String createR2DBCUrl() {
        return "r2dbc:tc:postgresql:///db?TC_IMAGE_TAG=10-alpine";
    }

    @Test
    public void testGetR2DBCUrl() {
        try (PostgreSQLContainer<?> container = createContainer()) {
            container.start();

            String expectedUrl =
                "r2dbc:postgresql://" +
                container.getHost() +
                ":" +
                container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT) +
                "/" +
                container.getDatabaseName() +
                container.constructUrlParameters("?", "&");

            ConnectionFactory connectionFactory = ConnectionFactories.get(getOptions(container));
            runTestQuery(connectionFactory);

            String r2dbcUrl = PostgreSQLR2DBCDatabaseContainer.getR2dbcUrl(container);
            assertThat(expectedUrl).isEqualTo(r2dbcUrl);
        }
    }
}
