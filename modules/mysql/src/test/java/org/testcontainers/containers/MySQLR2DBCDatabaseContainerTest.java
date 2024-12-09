package org.testcontainers.containers;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.junit.Test;
import org.testcontainers.MySQLTestImages;
import org.testcontainers.r2dbc.AbstractR2DBCDatabaseContainerTest;

import static org.assertj.core.api.Assertions.assertThat;

public class MySQLR2DBCDatabaseContainerTest extends AbstractR2DBCDatabaseContainerTest<MySQLContainer<?>> {

    @Override
    protected ConnectionFactoryOptions getOptions(MySQLContainer<?> container) {
        return MySQLR2DBCDatabaseContainer.getOptions(container);
    }

    @Override
    protected String createR2DBCUrl() {
        return "r2dbc:tc:mysql:///db?TC_IMAGE_TAG=" + MySQLTestImages.MYSQL_80_IMAGE.getVersionPart();
    }

    @Override
    protected MySQLContainer<?> createContainer() {
        return new MySQLContainer<>(MySQLTestImages.MYSQL_80_IMAGE);
    }

    @Test
    public void testGetR2DBCUrl() {
        try (MySQLContainer<?> container = createContainer()) {
            container.start();

            String expectedUrl =
                "r2dbc:mysql://" +
                container.getHost() +
                ":" +
                container.getMappedPort(MySQLContainer.MYSQL_PORT) +
                "/" +
                container.getDatabaseName() +
                container.constructUrlParameters("?", "&");

            ConnectionFactory connectionFactory = ConnectionFactories.get(getOptions(container));
            runTestQuery(connectionFactory);

            String r2dbcUrl = MySQLR2DBCDatabaseContainer.getR2dbcUrl(container);
            assertThat(expectedUrl).isEqualTo(r2dbcUrl);
        }
    }
}
