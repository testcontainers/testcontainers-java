package org.testcontainers.utility;

import com.google.common.base.MoreObjects;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * Provides a mechanism for fetching configuration/defaults from the classpath.
 */
@Data
@Slf4j @NoArgsConstructor(access = AccessLevel.PRIVATE)
public class TestcontainersConfiguration {

    @Getter(lazy = true)
    private static final TestcontainersConfiguration instance = loadConfiguration();

    private String ambassadorContainerImage = "richnorth/ambassador:latest";
    private String vncRecordedContainerImage = "richnorth/vnc-recorder:latest";
    private String tinyImage = "alpine:3.2";

    private static TestcontainersConfiguration loadConfiguration() {
        final TestcontainersConfiguration config = new TestcontainersConfiguration();

        ClassLoader loader = MoreObjects.firstNonNull(
                Thread.currentThread().getContextClassLoader(),
                TestcontainersConfiguration.class.getClassLoader());
        final URL configOverrides = loader.getResource("testcontainers.properties");
        if (configOverrides != null) {

            log.debug("Testcontainers configuration overrides will be loaded from {}", configOverrides);

            final Properties properties = new Properties();
            try (final InputStream inputStream = configOverrides.openStream()) {
                properties.load(inputStream);

                config.ambassadorContainerImage = properties.getProperty("ambassador.container.image", config.ambassadorContainerImage);
                config.vncRecordedContainerImage = properties.getProperty("vncrecorder.container.image", config.vncRecordedContainerImage);
                config.tinyImage = properties.getProperty("tinyimage.container.image", config.tinyImage);

                log.debug("Testcontainers configuration overrides loaded from {}: {}", configOverrides, config);

            } catch (IOException e) {
                log.error("Testcontainers config override was found on classpath but could not be loaded", e);
            }
        }

        return config;
    }
}
