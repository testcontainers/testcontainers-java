package org.testcontainers.utility;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.MalformedURLException;
import java.util.Objects;
import java.util.Properties;
import java.util.stream.Stream;

/**
 * Provides a mechanism for fetching configuration/defaults from the classpath.
 */
@Data
@Slf4j
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class TestcontainersConfiguration {

    private static String PROPERTIES_FILE_NAME = "testcontainers.properties";

    private static File GLOBAL_CONFIG_FILE = new File(System.getProperty("user.home"), "." + PROPERTIES_FILE_NAME);

    @Getter(lazy = true)
    private static final TestcontainersConfiguration instance = loadConfiguration();

    private final Properties properties;

    public String getAmbassadorContainerImage() {
        return (String) properties.getOrDefault("ambassador.container.image", "richnorth/ambassador:latest");
    }

    public String getSocatContainerImage() {
        return (String) properties.getOrDefault("socat.container.image", "alpine/socat:latest");
    }

    public String getVncRecordedContainerImage() {
        return (String) properties.getOrDefault("vncrecorder.container.image", "richnorth/vnc-recorder:latest");
    }

    public String getDockerComposeContainerImage() {
        return (String) properties.getOrDefault("compose.container.image", "docker/compose:1.8.0");
    }

    public String getTinyImage() {
        return (String) properties.getOrDefault("tinyimage.container.image", "alpine:3.5");
    }

    public String getRyukImage() {
        return (String) properties.getOrDefault("ryuk.container.image", "bsideup/moby-ryuk:0.2.2");
    }

    public Integer getRyukTimeout() {
        return (Integer) properties.getOrDefault("ryuk.container.timeout", 30);
    }

    public String getKafkaImage() {
        return (String) properties.getOrDefault("kafka.container.image", "confluentinc/cp-kafka");
    }

    public boolean isDisableChecks() {
        return Boolean.parseBoolean((String) properties.getOrDefault("checks.disable", "false"));
    }

    public String getDockerClientStrategyClassName() {
        return (String) properties.get("docker.client.strategy");
    }

    @Synchronized
    public boolean updateGlobalConfig(@NonNull String prop, @NonNull String value) {
        try {
            Properties globalProperties = new Properties();
            GLOBAL_CONFIG_FILE.createNewFile();
            try (InputStream inputStream = new FileInputStream(GLOBAL_CONFIG_FILE)) {
                globalProperties.load(inputStream);
            }

            if (value.equals(globalProperties.get(prop))) {
                return false;
            }

            globalProperties.setProperty(prop, value);

            try (OutputStream outputStream = new FileOutputStream(GLOBAL_CONFIG_FILE)) {
                globalProperties.store(outputStream, "Modified by Testcontainers");
            }

            // Update internal state only if global config was successfully updated
            properties.setProperty(prop, value);
            return true;
        } catch (Exception e) {
            log.debug("Can't store global property {} in {}", prop, GLOBAL_CONFIG_FILE);
            return false;
        }
    }

    @SneakyThrows(MalformedURLException.class)
    private static TestcontainersConfiguration loadConfiguration() {
        final TestcontainersConfiguration config = new TestcontainersConfiguration(
                Stream
                        .of(
                                TestcontainersConfiguration.class.getClassLoader().getResource(PROPERTIES_FILE_NAME),
                                Thread.currentThread().getContextClassLoader().getResource(PROPERTIES_FILE_NAME),
                                GLOBAL_CONFIG_FILE.toURI().toURL()
                        )
                        .filter(Objects::nonNull)
                        .map(it -> {
                            log.debug("Testcontainers configuration overrides will be loaded from {}", it);

                            final Properties subProperties = new Properties();
                            try (final InputStream inputStream = it.openStream()) {
                                subProperties.load(inputStream);
                            } catch (FileNotFoundException e) {
                                log.trace("Testcontainers config override was found on " + it + " but the file was not found", e);
                            } catch (IOException e) {
                                log.warn("Testcontainers config override was found on " + it + " but could not be loaded", e);
                            }
                            return subProperties;
                        })
                        .reduce(new Properties(), (a, b) -> {
                            a.putAll(b);
                            return a;
                        })
        );

        if (!config.getProperties().isEmpty()) {
            log.debug("Testcontainers configuration overrides loaded from {}", config);
        }

        return config;
    }
}
