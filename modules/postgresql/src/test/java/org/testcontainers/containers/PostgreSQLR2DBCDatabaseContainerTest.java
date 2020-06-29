package org.testcontainers.containers;

import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.PostgreSQLTestImages;
import org.testcontainers.r2dbc.AbstractR2DBCDatabaseContainerTest;

public class PostgreSQLR2DBCDatabaseContainerTest extends AbstractR2DBCDatabaseContainerTest<PostgreSQLContainer<?>> {

    @Override
    protected PostgreSQLContainer<?> createContainer() {
        return new PostgreSQLContainer<>(PostgreSQLTestImages.POSTGRES_TEST_IMAGE);
    }

    @Override
    protected ConnectionFactoryOptions getOptions(PostgreSQLContainer<?> container) {
        // get_options {
        ConnectionFactoryOptions options = PostgreSQLR2DBCDatabaseContainer.getOptions(
            container
        );
        // }

        return options;
    }

    protected String createR2DBCUrl() {
        return "r2dbc:tc:postgresql:///db?TC_IMAGE_TAG=10-alpine";
    }
}
