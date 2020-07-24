package org.testcontainers.containers;

import com.google.auto.service.AutoService;
import dev.miku.r2dbc.mysql.MySqlConnectionFactoryProvider;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.r2dbc.AbstractR2DBCDatabaseContainerProvider;
import org.testcontainers.r2dbc.R2DBCDatabaseContainer;
import org.testcontainers.r2dbc.R2DBCDatabaseContainerProvider;

import javax.annotation.Nullable;

@AutoService(R2DBCDatabaseContainerProvider.class)
public class MySQLR2DBCDatabaseContainerProvider extends AbstractR2DBCDatabaseContainerProvider {

    static final String DRIVER = MySqlConnectionFactoryProvider.MYSQL_DRIVER;

    public MySQLR2DBCDatabaseContainerProvider() {
        super(DRIVER, MySQLContainer.IMAGE);
    }

    @Override
    public R2DBCDatabaseContainer doCreateContainer(ConnectionFactoryOptions options) {
        String image = getImageString(options);
        MySQLContainer<?> container = new MySQLContainer<>(image)
            .withDatabaseName(options.getRequiredValue(ConnectionFactoryOptions.DATABASE));

        if (Boolean.TRUE.equals(options.getValue(REUSABLE_OPTION))) {
            container.withReuse(true);
        }
        return new MySQLR2DBCDatabaseContainer(container);
    }

    @Nullable
    @Override
    public ConnectionFactoryMetadata getMetadata(ConnectionFactoryOptions options) {
        ConnectionFactoryOptions.Builder builder = options.mutate();
        if (!options.hasOption(ConnectionFactoryOptions.USER)) {
            builder.option(ConnectionFactoryOptions.USER, MySQLContainer.DEFAULT_USER);
        }
        if (!options.hasOption(ConnectionFactoryOptions.PASSWORD)) {
            builder.option(ConnectionFactoryOptions.PASSWORD, MySQLContainer.DEFAULT_PASSWORD);
        }
        return super.getMetadata(builder.build());
    }
}
