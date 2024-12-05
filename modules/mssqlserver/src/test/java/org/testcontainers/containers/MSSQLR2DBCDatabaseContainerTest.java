package org.testcontainers.containers;

import io.r2dbc.spi.ConnectionFactoryOptions;
import org.junit.Test;
import org.testcontainers.MSSQLServerTestImages;
import org.testcontainers.r2dbc.AbstractR2DBCDatabaseContainerTest;

import static org.assertj.core.api.Assertions.assertThat;

public class MSSQLR2DBCDatabaseContainerTest extends AbstractR2DBCDatabaseContainerTest<MSSQLServerContainer<?>> {

    @Override
    protected ConnectionFactoryOptions getOptions(MSSQLServerContainer<?> container) {
        return MSSQLR2DBCDatabaseContainer.getOptions(container);
    }

    @Override
    protected String createR2DBCUrl() {
        return "r2dbc:tc:sqlserver:///?TC_IMAGE_TAG=2022-CU14-ubuntu-22.04";
    }

    @Override
    protected MSSQLServerContainer<?> createContainer() {
        return new MSSQLServerContainer<>(MSSQLServerTestImages.MSSQL_SERVER_IMAGE);
    }

    @Test
    public void testGetR2DBCUrl() {
        MSSQLServerContainer<?> container = createContainer();
        container.start();

        String expectedUrl =
            "r2dbc:sqlserver://" +
            container.getHost() +
            ":" +
            container.getMappedPort(MSSQLServerContainer.MS_SQL_SERVER_PORT) +
            container.constructUrlParameters(";", ";");

        String r2dbcUrl = MSSQLR2DBCDatabaseContainer.getR2dbcUrl(container);
        assertThat(expectedUrl).isEqualTo(r2dbcUrl);
        container.stop();
    }
}
