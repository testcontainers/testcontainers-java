package org.testcontainers.r2dbc;

import io.r2dbc.spi.ConnectionFactoryOptions;
import lombok.NonNull;
import org.jetbrains.annotations.NotNull;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.Properties;

public abstract class AbstractR2DBCDatabaseContainerProvider implements R2DBCDatabaseContainerProvider {

    final String originalDriver;
    final String defaultImage;

    protected AbstractR2DBCDatabaseContainerProvider(@NonNull String originalDriver, @NonNull String defaultImage) {
        this.originalDriver = originalDriver;
        this.defaultImage = defaultImage;
    }

    @Override
    public boolean supports(ConnectionFactoryOptions options) {
        return originalDriver.equals(getDriver(options));
    }

    protected String getImageString(ConnectionFactoryOptions options) {
        return String.format(
            "%s:%s",
            options.hasOption(IMAGE_OPTION)
                ? options.getValue(IMAGE_OPTION)
                : defaultImage,
            options.getRequiredValue(IMAGE_TAG_OPTION)
        );
    }

    private String getDriver(ConnectionFactoryOptions options) {
        Properties properties = TestcontainersConfiguration.getInstance().getProperties();
        return properties.getProperty(
            driverPropertyName(options.getRequiredValue(ConnectionFactoryOptions.DRIVER)),
            options.getRequiredValue(ConnectionFactoryOptions.DRIVER)
        );
    }

    private String driverPropertyName(String alias) {
        return String.format("db.alias.%s.r2dbcDriver", alias);
    }

    @Override
    public final R2DBCDatabaseContainer createContainer(ConnectionFactoryOptions options) {
        options = updateOptionsWithAliasImageIfSet(options);
        R2DBCDatabaseContainer container = doCreateContainer(options);

        return new R2DBCDatabaseContainer() {
            @Override
            public ConnectionFactoryOptions configure(ConnectionFactoryOptions options) {
                options = options.mutate()
                    .option(ConnectionFactoryOptions.DRIVER, originalDriver)
                    .build();
                return container.configure(options);
            }

            @Override
            public void start() {
                container.start();
            }

            @Override
            public void stop() {
                container.stop();
            }
        };
    }

    @NotNull
    private ConnectionFactoryOptions updateOptionsWithAliasImageIfSet(ConnectionFactoryOptions options) {
        String driver = options.getRequiredValue(ConnectionFactoryOptions.DRIVER);

        Properties properties = TestcontainersConfiguration.getInstance().getProperties();
        if (properties.containsKey(driverPropertyName(driver))) {
            String imagePropertyName = String.format("db.alias.%s.image", driver);
            String image = properties.getProperty(imagePropertyName);
            if (image == null) {
                throw new IllegalArgumentException(String.format("Property '%s' is not set", imagePropertyName));
            }

            options = options.mutate()
                .option(IMAGE_OPTION, image)
                .build();
        }
        return options;
    }

    protected abstract R2DBCDatabaseContainer doCreateContainer(ConnectionFactoryOptions options);
}
