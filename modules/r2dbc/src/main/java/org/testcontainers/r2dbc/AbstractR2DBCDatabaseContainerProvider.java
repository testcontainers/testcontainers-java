package org.testcontainers.r2dbc;

import io.r2dbc.spi.ConnectionFactoryOptions;
import lombok.NonNull;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.Properties;

public abstract class AbstractR2DBCDatabaseContainerProvider implements R2DBCDatabaseContainerProvider {

    final String originalDriver;

    protected AbstractR2DBCDatabaseContainerProvider(@NonNull String originalDriver) {
        this.originalDriver = originalDriver;
    }

    @Override
    public boolean supports(ConnectionFactoryOptions options) {
        return originalDriver.equals(getDriver(options));
    }

    private String getDriver(ConnectionFactoryOptions options) {
        Properties properties = TestcontainersConfiguration.getInstance().getProperties();
        return properties.getProperty(
            driverPropertyName(options.getRequiredValue(ConnectionFactoryOptions.DRIVER)),
            originalDriver
        );
    }

    private String driverPropertyName(String alias) {
        return String.format("db.alias.%s.r2dbcDriver", alias);
    }

    @Override
    public final R2DBCDatabaseContainer createContainer(ConnectionFactoryOptions options) {
        String driver = options.getRequiredValue(ConnectionFactoryOptions.DRIVER);

        Properties properties = TestcontainersConfiguration.getInstance().getProperties();
        if (properties.containsKey(driverPropertyName(driver))) {
            String imagePropertyName = String.format("db.alias.%s.image", driver);
            String image = properties.getProperty(imagePropertyName);
            if (image == null) {
                throw new IllegalArgumentException(String.format("Property '%s' is not set", imagePropertyName));
            }

            options = options.mutate()
                .option(ConnectionFactoryOptions.DRIVER, originalDriver)
                .option(IMAGE_OPTION, image)
                .build();
        }

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

    protected abstract R2DBCDatabaseContainer doCreateContainer(ConnectionFactoryOptions options);
}
