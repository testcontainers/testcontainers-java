package org.testcontainers.mysql;

import static org.assertj.core.api.Assertions.assertThat;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.junit.jupiter.api.Test;
import org.testcontainers.MySQLTestImages;
import org.testcontainers.r2dbc.AbstractR2DBCDatabaseContainerTest;

public class MySQLR2DBCDatabaseContainerTest extends AbstractR2DBCDatabaseContainerTest<MySQLContainer> {

    @Override
    protected ConnectionFactoryOptions getOptions(MySQLContainer container) {
        return MySQLR2DBCDatabaseContainer.getOptions(container);
    }

    @Override
    protected String createR2DBCUrl() {
        return "r2dbc:tc:mysql:///db?TC_IMAGE_TAG=" + MySQLTestImages.MYSQL_80_IMAGE.getVersionPart();
    }

    @Override
    protected MySQLContainer createContainer() {
        return new MySQLContainer(MySQLTestImages.MYSQL_80_IMAGE);
    }

    @Test
    void testGetR2dbcUrl() {
        try (
            MySQLContainer container = new MySQLContainer(MySQLTestImages.MYSQL_80_IMAGE)
                .withDatabaseName("testdb")
                .withUsername("testuser")
                .withPassword("testpass")
        ) {
            container.start();
            String url = MySQLR2DBCDatabaseContainer.getR2dbcUrl(container);
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
