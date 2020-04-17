package org.testcontainers.containers;

import com.google.auto.service.AutoService;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.mariadb.r2dbc.MariadbConnectionFactoryProvider;
import org.testcontainers.r2dbc.AbstractR2DBCDatabaseContainerProvider;
import org.testcontainers.r2dbc.R2DBCDatabaseContainer;
import org.testcontainers.r2dbc.R2DBCDatabaseContainerProvider;

@AutoService(R2DBCDatabaseContainerProvider.class)
public class MariaDBR2DBCDatabaseContainerProvider extends AbstractR2DBCDatabaseContainerProvider {

    static final String DRIVER = MariadbConnectionFactoryProvider.MARIADB_DRIVER;

    public MariaDBR2DBCDatabaseContainerProvider() {
        super(DRIVER);
    }

    @Override
    public R2DBCDatabaseContainer doCreateContainer(ConnectionFactoryOptions options) {
        String image = String.format(
            "%s:%s",
            options.hasOption(IMAGE_OPTION)
                ? options.getValue(IMAGE_OPTION)
                : MariaDBContainer.IMAGE,
            options.getRequiredValue(IMAGE_TAG_OPTION)
        );
        MariaDBContainer<?> container = new MariaDBContainer<>(image)
            .withDatabaseName(options.getRequiredValue(ConnectionFactoryOptions.DATABASE));

        if (Boolean.TRUE.equals(options.getValue(REUSABLE_OPTION))) {
            container.withReuse(true);
        }
        return new MariaDBR2DBCDatabaseContainer(container);
    }
}
