package org.testcontainers.containers;

import com.google.auto.service.AutoService;
import io.r2dbc.mssql.MssqlConnectionFactoryProvider;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.r2dbc.R2DBCDatabaseContainer;
import org.testcontainers.r2dbc.R2DBCDatabaseContainerProvider;

@AutoService(R2DBCDatabaseContainerProvider.class)
public class MSSQLR2DBCDatabaseContainerProvider implements R2DBCDatabaseContainerProvider {

    static final String DRIVER = MssqlConnectionFactoryProvider.MSSQL_DRIVER;

    @Override
    public boolean supports(ConnectionFactoryOptions options) {
        return DRIVER.equals(options.getRequiredValue(ConnectionFactoryOptions.DRIVER));
    }

    @Override
    public R2DBCDatabaseContainer createContainer(ConnectionFactoryOptions options) {
        MSSQLServerContainer<?> container = new MSSQLServerContainer<>(options.getRequiredValue(IMAGE_OPTION));

        if (Boolean.TRUE.equals(options.getValue(REUSABLE_OPTION))) {
            container.withReuse(true);
        }
        return new MSSQLR2DBCDatabaseContainer(container);
    }
}
