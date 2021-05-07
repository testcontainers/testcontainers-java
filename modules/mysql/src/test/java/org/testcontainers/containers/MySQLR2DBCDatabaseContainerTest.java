package org.testcontainers.containers;

import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.MySQLTestImages;
import org.testcontainers.r2dbc.AbstractR2DBCDatabaseContainerTest;

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

}
