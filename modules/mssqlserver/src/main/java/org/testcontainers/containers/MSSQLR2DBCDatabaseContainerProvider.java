package org.testcontainers.containers;

import com.google.auto.service.AutoService;
import io.r2dbc.mssql.MssqlConnectionFactoryProvider;
import io.r2dbc.spi.ConnectionFactoryOptions;
import lombok.NonNull;
import org.testcontainers.r2dbc.AbstractR2DBCDatabaseContainerProvider;
import org.testcontainers.r2dbc.R2DBCDatabaseContainer;
import org.testcontainers.r2dbc.R2DBCDatabaseContainerProvider;

@AutoService(R2DBCDatabaseContainerProvider.class)
public class MSSQLR2DBCDatabaseContainerProvider extends AbstractR2DBCDatabaseContainerProvider {

    static final String DRIVER = MssqlConnectionFactoryProvider.MSSQL_DRIVER;

    public MSSQLR2DBCDatabaseContainerProvider() {
        super(DRIVER);
    }

    @Override
    public R2DBCDatabaseContainer doCreateContainer(ConnectionFactoryOptions options) {
        String image = String.format(
            "%s:%s",
            options.hasOption(IMAGE_OPTION)
                ? options.getValue(IMAGE_OPTION)
                : MSSQLServerContainer.IMAGE,
            options.getRequiredValue(IMAGE_TAG_OPTION)
        );
        MSSQLServerContainer<?> container = new MSSQLServerContainer<>(image);

        if (Boolean.TRUE.equals(options.getValue(REUSABLE_OPTION))) {
            container.withReuse(true);
        }
        return new MSSQLR2DBCDatabaseContainer(container);
    }
}
