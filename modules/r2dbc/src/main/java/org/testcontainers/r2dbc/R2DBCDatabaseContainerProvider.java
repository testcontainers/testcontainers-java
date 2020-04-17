package org.testcontainers.r2dbc;

import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.Option;

public interface R2DBCDatabaseContainerProvider {

    Option<Boolean> REUSABLE_OPTION = Option.valueOf("TC_REUSABLE");

    Option<String> IMAGE_TAG_OPTION = Option.valueOf("TC_IMAGE_TAG");

    Option<String> IMAGE_OPTION = Option.valueOf("TC_IMAGE");

    boolean supports(ConnectionFactoryOptions options);

    R2DBCDatabaseContainer createContainer(ConnectionFactoryOptions options);
}
