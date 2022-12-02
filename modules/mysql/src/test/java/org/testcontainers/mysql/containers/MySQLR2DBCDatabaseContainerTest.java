package org.testcontainers.mysql.containers;

import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.mysql.MySQLTestImages;
import org.testcontainers.mysql.MySQLContainer;
import org.testcontainers.mysql.MySQLR2DBCDatabaseContainer;
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
