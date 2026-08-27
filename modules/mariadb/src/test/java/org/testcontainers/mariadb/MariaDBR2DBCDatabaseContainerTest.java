package org.testcontainers.mariadb;

import static org.assertj.core.api.Assertions.assertThat;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.junit.jupiter.api.Test;
import org.testcontainers.r2dbc.AbstractR2DBCDatabaseContainerTest;
import org.testcontainers.utility.DockerImageName;

public class MariaDBR2DBCDatabaseContainerTest extends AbstractR2DBCDatabaseContainerTest<MariaDBContainer> {

    @Override
    protected ConnectionFactoryOptions getOptions(MariaDBContainer container) {
        return MariaDBR2DBCDatabaseContainer.getOptions(container);
    }

    @Override
    protected String createR2DBCUrl() {
        return "r2dbc:tc:mariadb:///db?TC_IMAGE_TAG=10.3.39";
    }

    @Override
    protected MariaDBContainer createContainer() {
        return new MariaDBContainer(DockerImageName.parse("mariadb:10.3.39"));
    }

    @Test
    void testGetR2dbcUrl() {
        try (
            MariaDBContainer container = new MariaDBContainer(DockerImageName.parse("mariadb:10.3.39"))
                .withDatabaseName("testdb")
                .withUsername("testuser")
                .withPassword("testpass")
        ) {
            container.start();
            String url = MariaDBR2DBCDatabaseContainer.getR2dbcUrl(container);
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
