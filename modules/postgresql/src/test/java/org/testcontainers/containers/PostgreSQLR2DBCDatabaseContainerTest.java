package org.testcontainers.containers;

import static org.assertj.core.api.Assertions.assertThat;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.junit.jupiter.api.Test;
import org.testcontainers.PostgreSQLTestImages;
import org.testcontainers.r2dbc.AbstractR2DBCDatabaseContainerTest;

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
    void testGetR2dbcUrl() {
        try (
            PostgreSQLContainer<?> container = new PostgreSQLContainer<>(PostgreSQLTestImages.POSTGRES_TEST_IMAGE)
                .withDatabaseName("testdb")
                .withUsername("testuser")
                .withPassword("testpass")
        ) {
            container.start();
            String url = PostgreSQLR2DBCDatabaseContainer.getR2dbcUrl(container);
            assertThat(url).contains("/testdb");
            ConnectionFactory connectionFactory = ConnectionFactories.get(
                ConnectionFactoryOptions
                    .parse(url)
                    .mutate()
                    .option(ConnectionFactoryOptions.USER, container.getUsername())
                    .option(ConnectionFactoryOptions.PASSWORD, container.getPassword())
                    .build()
            );
            runTestQuery(connectionFactory);
        }
    }
}
