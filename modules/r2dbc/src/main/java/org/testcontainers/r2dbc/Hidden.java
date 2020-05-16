package org.testcontainers.r2dbc;

import com.google.auto.service.AutoService;
import io.r2dbc.spi.ConnectionFactory;
import io.r2dbc.spi.ConnectionFactoryOptions;
import io.r2dbc.spi.ConnectionFactoryProvider;

/**
 * Hide inner classes that must be public due to the way {@link java.util.ServiceLoader} works
 */
class Hidden {

    @AutoService(ConnectionFactoryProvider.class)
    public static final class TestcontainersR2DBCConnectionFactoryProvider implements ConnectionFactoryProvider {

        public static final String DRIVER = "tc";

        @Override
        public ConnectionFactory create(ConnectionFactoryOptions options) {
            options = sanitize(options);
            options = removeProxying(options);

            return new TestcontainersR2DBCConnectionFactory(options);
        }

        private ConnectionFactoryOptions sanitize(ConnectionFactoryOptions options) {
            ConnectionFactoryOptions.Builder builder = options.mutate();

            Object reusable = options.getValue(R2DBCDatabaseContainerProvider.REUSABLE_OPTION);
            if (reusable instanceof String) {
                builder.option(R2DBCDatabaseContainerProvider.REUSABLE_OPTION, Boolean.valueOf((String) reusable));
            }
            return builder.build();
        }

        private ConnectionFactoryOptions removeProxying(ConnectionFactoryOptions options) {
            // To delegate to the next factory provider, inspect the PROTOCOL and convert it to the next DRIVER and PROTOCOL values.
            //
            // example:
            //   | Property | Input           | Output       |
            //   |----------|-----------------|--------------|
            //   | DRIVER   | tc              | postgres     |
            //   | PROTOCOL | postgres        | <empty>      |

            String protocol = options.getRequiredValue(ConnectionFactoryOptions.PROTOCOL);
            if (protocol.trim().length() == 0) {
                throw new IllegalArgumentException("Invalid protocol: " + protocol);
            }
            String[] protocols = protocol.split(":", 2);
            String driverDelegate = protocols[0];

            // when protocol does NOT contain COLON, the length becomes 1
            String protocolDelegate = protocols.length == 2 ? protocols[1] : "";

            return ConnectionFactoryOptions.builder()
                .from(options)
                .option(ConnectionFactoryOptions.DRIVER, driverDelegate)
                .option(ConnectionFactoryOptions.PROTOCOL, protocolDelegate)
                .build();
        }

        @Override
        public boolean supports(ConnectionFactoryOptions options) {
            return DRIVER.equals(options.getValue(ConnectionFactoryOptions.DRIVER));
        }

        @Override
        public String getDriver() {
            return DRIVER;
        }

    }
}
