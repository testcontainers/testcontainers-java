package org.testcontainers.containers;

import com.google.auto.service.AutoService;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.mariadb.r2dbc.MariadbConnectionFactoryProvider;
import org.testcontainers.r2dbc.R2DBCDatabaseContainer;
import org.testcontainers.r2dbc.R2DBCDatabaseContainerProvider;

import javax.annotation.Nullable;

@AutoService(R2DBCDatabaseContainerProvider.class)
public class MariaDBR2DBCDatabaseContainerProvider implements R2DBCDatabaseContainerProvider {

    static final String DRIVER = MariadbConnectionFactoryProvider.MARIADB_DRIVER;

    @Override
    public boolean supports(ConnectionFactoryOptions options) {
        return DRIVER.equals(options.getRequiredValue(ConnectionFactoryOptions.DRIVER));
    }

    @Override
    public R2DBCDatabaseContainer createContainer(ConnectionFactoryOptions options) {
        String image = MariaDBContainer.IMAGE + ":" + options.getRequiredValue(IMAGE_TAG_OPTION);
        MariaDBContainer<?> container = new MariaDBContainer<>(image)
            .withDatabaseName(options.getRequiredValue(ConnectionFactoryOptions.DATABASE));

        if (Boolean.TRUE.equals(options.getValue(REUSABLE_OPTION))) {
            container.withReuse(true);
        }
        return new MariaDBR2DBCDatabaseContainer(container);
    }

    @Nullable
    @Override
    public ConnectionFactoryMetadata getMetadata(ConnectionFactoryOptions options) {
        ConnectionFactoryOptions.Builder builder = options.mutate();
        if (!options.hasOption(ConnectionFactoryOptions.USER)) {
            builder.option(ConnectionFactoryOptions.USER, MariaDBContainer.DEFAULT_USER);
        }
        if (!options.hasOption(ConnectionFactoryOptions.PASSWORD)) {
            builder.option(ConnectionFactoryOptions.PASSWORD, MariaDBContainer.DEFAULT_PASSWORD);
        }
        return R2DBCDatabaseContainerProvider.super.getMetadata(builder.build());
    }
}
