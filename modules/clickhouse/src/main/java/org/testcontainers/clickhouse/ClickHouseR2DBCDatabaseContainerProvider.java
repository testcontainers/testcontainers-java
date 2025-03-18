package org.testcontainers.clickhouse;

import com.clickhouse.r2dbc.connection.ClickHouseConnectionFactoryProvider;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.r2dbc.R2DBCDatabaseContainer;
import org.testcontainers.r2dbc.R2DBCDatabaseContainerProvider;

import javax.annotation.Nullable;

public class ClickHouseR2DBCDatabaseContainerProvider implements R2DBCDatabaseContainerProvider {

    static final String DRIVER = ClickHouseConnectionFactoryProvider.CLICKHOUSE_DRIVER;

    @Override
    public boolean supports(ConnectionFactoryOptions options) {
        return DRIVER.equals(options.getRequiredValue(ConnectionFactoryOptions.DRIVER));
    }

    @Override
    public R2DBCDatabaseContainer createContainer(ConnectionFactoryOptions options) {
        String image =
            ClickHouseContainer.CLICKHOUSE_CLICKHOUSE_SERVER + ":" + options.getRequiredValue(IMAGE_TAG_OPTION);
        ClickHouseContainer container = new ClickHouseContainer(image)
            .withDatabaseName((String) options.getRequiredValue(ConnectionFactoryOptions.DATABASE));

        if (Boolean.TRUE.equals(options.getValue(REUSABLE_OPTION))) {
            container.withReuse(true);
        }
        return new ClickHouseR2DBCDatabaseContainer(container);
    }

    @Nullable
    @Override
    public ConnectionFactoryMetadata getMetadata(ConnectionFactoryOptions options) {
        ConnectionFactoryOptions.Builder builder = options.mutate();
        if (!options.hasOption(ConnectionFactoryOptions.USER)) {
            builder.option(ConnectionFactoryOptions.USER, ClickHouseContainer.DEFAULT_USER);
        }
        if (!options.hasOption(ConnectionFactoryOptions.PASSWORD)) {
            builder.option(ConnectionFactoryOptions.PASSWORD, ClickHouseContainer.DEFAULT_PASSWORD);
        }
        builder.option(ConnectionFactoryOptions.PROTOCOL, "http");
        return R2DBCDatabaseContainerProvider.super.getMetadata(builder.build());
    }
}
