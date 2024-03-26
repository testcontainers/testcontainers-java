package org.testcontainers.containers;

import com.clickhouse.r2dbc.connection.ClickHouseConnectionFactoryProvider;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.clickhouse.ClickHouseContainer;
import org.testcontainers.clickhouse.ClickHouseR2DBCDatabaseContainer;
import org.testcontainers.r2dbc.R2DBCDatabaseContainer;
import org.testcontainers.r2dbc.R2DBCDatabaseContainerProvider;

public class ClickHouseR2DBCDatabaseContainerProvider implements R2DBCDatabaseContainerProvider {
    public static final String DRIVER = ClickHouseConnectionFactoryProvider.CLICKHOUSE_DRIVER;

    private static final String IMAGE_NAME = "clickhouse/clickhouse-server";

    @Override
    public boolean supports(ConnectionFactoryOptions options) {
        return DRIVER.equals(options.getRequiredValue(ConnectionFactoryOptions.DRIVER));
    }

    @Override
    public R2DBCDatabaseContainer createContainer(ConnectionFactoryOptions options) {
        String image = IMAGE_NAME + ":" + options.getRequiredValue(IMAGE_TAG_OPTION);
        ClickHouseContainer container = new ClickHouseContainer(image)
            .withDatabaseName((String) options.getRequiredValue(ConnectionFactoryOptions.DATABASE));

        if (Boolean.TRUE.equals(options.getValue(REUSABLE_OPTION))) {
            container.withReuse(true);
        }
        return new ClickHouseR2DBCDatabaseContainer(container);
    }
}
