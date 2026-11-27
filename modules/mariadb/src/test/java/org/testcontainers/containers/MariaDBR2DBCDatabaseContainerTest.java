package org.testcontainers.containers;

import io.r2dbc.spi.ConnectionFactoryOptions;
import org.junit.jupiter.api.Test;
import org.testcontainers.MariaDBTestImages;
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
    public void testGetR2dbcUrl() {
        try (MariaDBContainer<?> container = new MariaDBContainer<>(MariaDBTestImages.MARIADB_IMAGE)) {
            container.start();

            String r2dbcUrl = MariaDBR2DBCDatabaseContainer.getR2dbcUrl(container);

            String user = container.getUsername();
            String password = container.getPassword();
            String host = container.getHost();
            Integer port = container.getMappedPort(MariaDBContainer.MARIADB_PORT);
            String db = container.getDatabaseName();

            String expectedPattern = String.format("^r2dbc:mariadb://%s:%s@%s:%d/%s$", user, password, host, port, db);

            assertThat(r2dbcUrl)
                .as("URL must strictly match format 'r2dbc:mariadb://user:pass@host:port/db'")
                .matches(expectedPattern);
        }
    }
}
