package org.testcontainers.containers;

import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.MSSQLServerTestImages;
import org.testcontainers.r2dbc.AbstractR2DBCDatabaseContainerTest;

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
}
