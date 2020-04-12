package org.testcontainers.containers;

import com.google.auto.service.AutoService;
import io.r2dbc.postgresql.PostgresqlConnectionFactoryProvider;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.r2dbc.R2DBCDatabaseContainer;
import org.testcontainers.r2dbc.R2DBCDatabaseContainerProvider;

@AutoService(R2DBCDatabaseContainerProvider.class)
public final class PostgreSQLR2DBCDatabaseContainerProvider implements R2DBCDatabaseContainerProvider {

    static final String DRIVER = PostgresqlConnectionFactoryProvider.POSTGRESQL_DRIVER;

    @Override
    public boolean supports(ConnectionFactoryOptions options) {
        return DRIVER.equals(options.getRequiredValue(ConnectionFactoryOptions.DRIVER));
    }

    @Override
    public R2DBCDatabaseContainer createContainer(ConnectionFactoryOptions options) {
        String image = PostgreSQLContainer.IMAGE + ":" + options.getRequiredValue(IMAGE_TAG_OPTION);
        PostgreSQLContainer<?> container = new PostgreSQLContainer<>(image)
            .withDatabaseName(options.getRequiredValue(ConnectionFactoryOptions.DATABASE));

        if (Boolean.TRUE.equals(options.getValue(REUSABLE_OPTION))) {
            container.withReuse(true);
        }
        return new PostgreSQLR2DBCDatabaseContainer(container);
    }
}
