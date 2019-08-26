package org.testcontainers.utility;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
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

    private static File ENVIRONMENT_CONFIG_FILE = new File(System.getProperty("user.home"), "." + PROPERTIES_FILE_NAME);

    @Getter(lazy = true)
    private static final TestcontainersConfiguration instance = loadConfiguration();

    @Getter(AccessLevel.NONE)
    private final Properties environmentProperties;

    private final Properties properties = new Properties();

    TestcontainersConfiguration(Properties environmentProperties, Properties classpathProperties) {
        this.environmentProperties = environmentProperties;

        this.properties.putAll(classpathProperties);
        this.properties.putAll(environmentProperties);
    }

    public String getAmbassadorContainerImage() {
        return (String) properties.getOrDefault("ambassador.container.image", "richnorth/ambassador:latest");
    }

    public String getSocatContainerImage() {
        return (String) properties.getOrDefault("socat.container.image", "alpine/socat:latest");
    }

    public String getVncRecordedContainerImage() {
        return (String) properties.getOrDefault("vncrecorder.container.image", "quay.io/testcontainers/vnc-recorder:1.1.0");
    }

    public String getDockerComposeContainerImage() {
        return (String) properties.getOrDefault("compose.container.image", "docker/compose:1.8.0");
    }

    public String getTinyImage() {
        return (String) properties.getOrDefault("tinyimage.container.image", "alpine:3.5");
    }

    public boolean isRyukPrivileged() {
        return Boolean.parseBoolean((String) properties.getOrDefault("ryuk.container.privileged", "false"));
    }

    public String getRyukImage() {
        return (String) properties.getOrDefault("ryuk.container.image", "quay.io/testcontainers/ryuk:0.2.3");
    }

    public String getSSHdImage() {
        return (String) properties.getOrDefault("sshd.container.image", "quay.io/testcontainers/sshd:1.0.0");
    }

    public Integer getRyukTimeout() {
        return Integer.parseInt((String) properties.getOrDefault("ryuk.container.timeout", "30"));
    }

    public String getKafkaImage() {
        return (String) properties.getOrDefault("kafka.container.image", "confluentinc/cp-kafka");
    }

    public String getPulsarImage() {
        return (String) properties.getOrDefault("pulsar.container.image", "apachepulsar/pulsar");
    }

    public boolean isDisableChecks() {
        return Boolean.parseBoolean((String) environmentProperties.getOrDefault("checks.disable", "false"));
    }

    public String getDockerClientStrategyClassName() {
        return (String) environmentProperties.get("docker.client.strategy");
    }

    /**
     * 
     * @deprecated we no longer have different transport types
     */
    @Deprecated
    public String getTransportType() {
        return properties.getProperty("transport.type", "okhttp");
    }

    public Integer getImagePullPauseTimeout() {
        return Integer.parseInt((String) properties.getOrDefault("pull.pause.timeout", "30"));
    }

    @Synchronized
    public boolean updateGlobalConfig(@NonNull String prop, @NonNull String value) {
        try {
            if (value.equals(environmentProperties.get(prop))) {
                return false;
            }

            environmentProperties.setProperty(prop, value);

            ENVIRONMENT_CONFIG_FILE.createNewFile();
            try (OutputStream outputStream = new FileOutputStream(ENVIRONMENT_CONFIG_FILE)) {
                environmentProperties.store(outputStream, "Modified by Testcontainers");
            }

            // Update internal state only if environment config was successfully updated
            properties.setProperty(prop, value);
            return true;
        } catch (Exception e) {
            log.debug("Can't store environment property {} in {}", prop, ENVIRONMENT_CONFIG_FILE);
            return false;
        }
    }

    @SneakyThrows(MalformedURLException.class)
    private static TestcontainersConfiguration loadConfiguration() {
        return new TestcontainersConfiguration(
            readProperties(ENVIRONMENT_CONFIG_FILE.toURI().toURL()),
            Stream
                .of(
                    TestcontainersConfiguration.class.getClassLoader(),
                    Thread.currentThread().getContextClassLoader()
                )
                .map(it -> it.getResource(PROPERTIES_FILE_NAME))
                .filter(Objects::nonNull)
                .map(TestcontainersConfiguration::readProperties)
                .reduce(new Properties(), (a, b) -> {
                    a.putAll(b);
                    return a;
                })
        );
    }

    private static Properties readProperties(URL url) {
        log.debug("Testcontainers configuration overrides will be loaded from {}", url);
        Properties properties = new Properties();
        try (InputStream inputStream = url.openStream()) {
            properties.load(inputStream);
        } catch (FileNotFoundException e) {
            log.trace("Testcontainers config override was found on {} but the file was not found", url, e);
        } catch (IOException e) {
            log.warn("Testcontainers config override was found on {} but could not be loaded", url, e);
        }
        return properties;
    }
}
