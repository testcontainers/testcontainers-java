package org.testcontainers.containers;

import com.google.auto.service.AutoService;
import dev.miku.r2dbc.mysql.MySqlConnectionFactoryProvider;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.r2dbc.AbstractR2DBCDatabaseContainerProvider;
import org.testcontainers.r2dbc.R2DBCDatabaseContainer;
import org.testcontainers.r2dbc.R2DBCDatabaseContainerProvider;

@AutoService(R2DBCDatabaseContainerProvider.class)
public class MySQLR2DBCDatabaseContainerProvider extends AbstractR2DBCDatabaseContainerProvider {

    static final String DRIVER = MySqlConnectionFactoryProvider.MYSQL_DRIVER;

    public MySQLR2DBCDatabaseContainerProvider() {
        super(DRIVER);
    }

    @Override
    public R2DBCDatabaseContainer doCreateContainer(ConnectionFactoryOptions options) {
        String image = String.format(
            "%s:%s",
            options.hasOption(IMAGE_OPTION)
                ? options.getValue(IMAGE_OPTION)
                : MySQLContainer.IMAGE,
            options.getRequiredValue(IMAGE_TAG_OPTION)
        );
        MySQLContainer<?> container = new MySQLContainer<>(image)
            .withDatabaseName(options.getRequiredValue(ConnectionFactoryOptions.DATABASE));

        if (Boolean.TRUE.equals(options.getValue(REUSABLE_OPTION))) {
            container.withReuse(true);
        }
        return new MySQLR2DBCDatabaseContainer(container);
    }
}
