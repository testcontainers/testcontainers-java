package org.testcontainers.containers;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.junit.Test;
import org.testcontainers.r2dbc.AbstractR2DBCDatabaseContainerTest;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;

public class MariaDBR2DBCDatabaseContainerTest extends AbstractR2DBCDatabaseContainerTest<MariaDBContainer<?>> {

    @Override
    protected ConnectionFactoryOptions getOptions(MariaDBContainer<?> container) {
        return MariaDBR2DBCDatabaseContainer.getOptions(container);
    }

    @Override
    protected String createR2DBCUrl() {
        return "r2dbc:tc:mariadb:///db?TC_IMAGE_TAG=10.3.39";
    }

    @Override
    protected MariaDBContainer<?> createContainer() {
        return new MariaDBContainer<>(DockerImageName.parse("mariadb:10.3.39"));
    }

    @Test
    public void testGetR2DBCUrl() {
        try (MariaDBContainer<?> container = createContainer()) {
            container.start();

            String expectedUrl =
                "r2dbc:mariadb://" +
                container.getHost() +
                ":" +
                container.getMappedPort(MariaDBContainer.MARIADB_PORT) +
                "/" +
                container.getDatabaseName() +
                container.constructUrlParameters("?", "&");

            ConnectionFactory connectionFactory = ConnectionFactories.get(getOptions(container));
            runTestQuery(connectionFactory);

            String r2dbcUrl = MariaDBR2DBCDatabaseContainer.getR2dbcUrl(container);
            assertThat(expectedUrl).isEqualTo(r2dbcUrl);
        }
    }
}
