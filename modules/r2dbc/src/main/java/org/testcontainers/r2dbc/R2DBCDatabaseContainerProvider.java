package org.testcontainers.r2dbc;

import io.r2dbc.spi.ConnectionFactories;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryMetadata;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;

import javax.annotation.Nullable;

public interface R2DBCDatabaseContainerProvider {

    Option<Boolean> REUSABLE_OPTION = Option.valueOf("TC_REUSABLE");

    Option<String> IMAGE_TAG_OPTION = Option.valueOf("TC_IMAGE_TAG");

    boolean supports(ConnectionFactoryOptions options);

    R2DBCDatabaseContainer createContainer(ConnectionFactoryOptions options);

    @Nullable
    default ConnectionFactoryMetadata getMetadata(ConnectionFactoryOptions options) {
        ConnectionFactoryOptions.Builder builder = options.mutate();
        if (!options.hasOption(ConnectionFactoryOptions.HOST)) {
            builder.option(ConnectionFactoryOptions.HOST, "localhost");
        }
        if (!options.hasOption(ConnectionFactoryOptions.PORT)) {
            builder.option(ConnectionFactoryOptions.PORT, 65535);
        }

        ConnectionFactory connectionFactory = ConnectionFactories.find(builder.build());
        return connectionFactory != null ? connectionFactory.getMetadata() : null;
    }
}
