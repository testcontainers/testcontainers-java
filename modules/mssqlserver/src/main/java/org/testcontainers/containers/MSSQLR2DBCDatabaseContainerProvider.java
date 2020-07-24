package org.testcontainers.containers;

import com.google.auto.service.AutoService;
import io.r2dbc.mssql.MssqlConnectionFactoryProvider;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import io.r2dbc.spi.ConnectionFactoryOptions;
import org.testcontainers.r2dbc.AbstractR2DBCDatabaseContainerProvider;
import org.testcontainers.r2dbc.R2DBCDatabaseContainer;
import org.testcontainers.r2dbc.R2DBCDatabaseContainerProvider;

import javax.annotation.Nullable;

@AutoService(R2DBCDatabaseContainerProvider.class)
public class MSSQLR2DBCDatabaseContainerProvider extends AbstractR2DBCDatabaseContainerProvider {

    static final String DRIVER = MssqlConnectionFactoryProvider.MSSQL_DRIVER;

    public MSSQLR2DBCDatabaseContainerProvider() {
        super(DRIVER, MSSQLServerContainer.IMAGE);
    }

    @Override
    public R2DBCDatabaseContainer doCreateContainer(ConnectionFactoryOptions options) {
        String image = getImageString(options);
        MSSQLServerContainer<?> container = new MSSQLServerContainer<>(image);

        if (Boolean.TRUE.equals(options.getValue(REUSABLE_OPTION))) {
            container.withReuse(true);
        }
        return new MSSQLR2DBCDatabaseContainer(container);
    }

    @Nullable
    @Override
    public ConnectionFactoryMetadata getMetadata(ConnectionFactoryOptions options) {
        ConnectionFactoryOptions.Builder builder = options.mutate();
        if (!options.hasOption(ConnectionFactoryOptions.USER)) {
            builder.option(ConnectionFactoryOptions.USER, MSSQLServerContainer.DEFAULT_USER);
        }
        if (!options.hasOption(ConnectionFactoryOptions.PASSWORD)) {
            builder.option(ConnectionFactoryOptions.PASSWORD, MSSQLServerContainer.DEFAULT_PASSWORD);
        }
        return super.getMetadata(builder.build());
    }
}
