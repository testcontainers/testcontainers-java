package org.testcontainers.postgresql;

import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.PostgreSQLTestImages;
import org.testcontainers.r2dbc.AbstractR2DBCDatabaseContainerTest;

public class PostgreSQLR2DBCDatabaseContainerTest extends AbstractR2DBCDatabaseContainerTest<PostgreSQLContainer> {

    @Override
    protected PostgreSQLContainer createContainer() {
        return new PostgreSQLContainer(PostgreSQLTestImages.POSTGRES_TEST_IMAGE);
    }

    @Override
    protected ConnectionFactoryOptions getOptions(PostgreSQLContainer container) {
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

    @org.junit.jupiter.api.Test
    void testGetR2dbcUrl() {
        try (PostgreSQLContainer container = createContainer()) {
            container.start();

            // Test static method
            String r2dbcUrlStatic = PostgreSQLR2DBCDatabaseContainer.getR2dbcUrl(container);

            org.assertj.core.api.Assertions.assertThat(r2dbcUrlStatic).isNotNull();
            org.assertj.core.api.Assertions.assertThat(r2dbcUrlStatic).startsWith("r2dbc:postgresql://");
            org.assertj.core.api.Assertions.assertThat(r2dbcUrlStatic).contains(container.getHost());
            org.assertj.core.api.Assertions.assertThat(r2dbcUrlStatic).contains(String.valueOf(container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT)));
            org.assertj.core.api.Assertions.assertThat(r2dbcUrlStatic).contains(container.getDatabaseName());
            org.assertj.core.api.Assertions.assertThat(r2dbcUrlStatic).contains(container.getUsername());
            org.assertj.core.api.Assertions.assertThat(r2dbcUrlStatic).contains(container.getPassword());

            // Verify the format: r2dbc:postgresql://username:password@host:port/database
            String expectedUrl = String.format(
                "r2dbc:postgresql://%s:%s@%s:%d/%s",
                container.getUsername(),
                container.getPassword(),
                container.getHost(),
                container.getMappedPort(PostgreSQLContainer.POSTGRESQL_PORT),
                container.getDatabaseName()
            );
            org.assertj.core.api.Assertions.assertThat(r2dbcUrlStatic).isEqualTo(expectedUrl);

            // Test instance method
            PostgreSQLR2DBCDatabaseContainer r2dbcContainer = new PostgreSQLR2DBCDatabaseContainer(container);
            String r2dbcUrlInstance = r2dbcContainer.getR2dbcUrl();

            org.assertj.core.api.Assertions.assertThat(r2dbcUrlInstance).isEqualTo(r2dbcUrlStatic);
        }
    }
}
