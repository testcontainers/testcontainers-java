package org.testcontainers.utility;

import com.google.common.annotations.VisibleForTesting;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.Synchronized;
import lombok.extern.slf4j.Slf4j;
import org.testcontainers.UnstableAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
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

    @SuppressWarnings({"ConstantConditions", "unchecked", "rawtypes"})
    @VisibleForTesting
    static AtomicReference<TestcontainersConfiguration> getInstanceField() {
        // Lazy Getter from Lombok changes the field's type to AtomicReference
        return (AtomicReference) (Object) instance;
    }

    @Getter(AccessLevel.NONE)
    private final Properties environmentProperties;

    private final Properties properties = new Properties();

    TestcontainersConfiguration(Properties environmentProperties, Properties classpathProperties) {
        this.environmentProperties = environmentProperties;

        this.properties.putAll(classpathProperties);
        this.properties.putAll(environmentProperties);
    }

    private DockerImageName getImage(final String key, final String defaultValue) {
        return DockerImageName
            .parse(properties.getProperty(key, defaultValue).trim())
            .asCompatibleSubstituteFor(defaultValue);
    }

    @Deprecated
    public String getAmbassadorContainerImage() {
        return getAmbassadorContainerDockerImageName().asCanonicalNameString();
    }

    @Deprecated
    public DockerImageName getAmbassadorContainerDockerImageName() {
        return getImage("ambassador.container.image", "richnorth/ambassador:latest");
    }

    @Deprecated
    public String getSocatContainerImage() {
        return getSocatDockerImageName().asCanonicalNameString();
    }

    public DockerImageName getSocatDockerImageName() {
        return getImage("socat.container.image", "alpine/socat:latest");
    }

    @Deprecated
    public String getVncRecordedContainerImage() {
        return getVncDockerImageName().asCanonicalNameString();
    }

    public DockerImageName getVncDockerImageName() {
        return getImage("vncrecorder.container.image", "testcontainers/vnc-recorder:1.1.0");
    }

    @Deprecated
    public String getDockerComposeContainerImage() {
        return getDockerComposeDockerImageName().asCanonicalNameString();
    }

    public DockerImageName getDockerComposeDockerImageName() {
        return getImage("compose.container.image", "docker/compose:1.24.1");
    }

    @Deprecated
    public String getTinyImage() {
        return getTinyDockerImageName().asCanonicalNameString();
    }

    public DockerImageName getTinyDockerImageName() {
        return getImage("tinyimage.container.image", "alpine:3.5");
    }

    public boolean isRyukPrivileged() {
        return Boolean.parseBoolean((String) properties.getOrDefault("ryuk.container.privileged", "false"));
    }

    @Deprecated
    public String getRyukImage() {
        return getRyukDockerImageName().asCanonicalNameString();
    }

    public DockerImageName getRyukDockerImageName() {
        return getImage("ryuk.container.image", "testcontainers/ryuk:0.3.0");
    }

    @Deprecated
    public String getSSHdImage() {
        return getSSHdDockerImageName().asCanonicalNameString();
    }

    public DockerImageName getSSHdDockerImageName() {
        return getImage("sshd.container.image", "testcontainers/sshd:1.0.0");
    }

    public Integer getRyukTimeout() {
        return Integer.parseInt((String) properties.getOrDefault("ryuk.container.timeout", "30"));
    }

    @Deprecated
    public String getKafkaImage() {
        return getKafkaDockerImageName().asCanonicalNameString();
    }

    public DockerImageName getKafkaDockerImageName() {
        return getImage("kafka.container.image", "confluentinc/cp-kafka");
    }

    @Deprecated
    public String getPulsarImage() {
        return getPulsarDockerImageName().asCanonicalNameString();
    }

    public DockerImageName getPulsarDockerImageName() {
        return getImage("pulsar.container.image", "apachepulsar/pulsar");
    }

    @Deprecated
    public String getLocalStackImage() {
        return getLocalstackDockerImageName().asCanonicalNameString();
    }

    public DockerImageName getLocalstackDockerImageName() {
        return getImage("localstack.container.image", "localstack/localstack");
    }

    public boolean isDisableChecks() {
        return Boolean.parseBoolean((String) environmentProperties.getOrDefault("checks.disable", "false"));
    }

    @UnstableAPI
    public boolean environmentSupportsReuse() {
        return Boolean.parseBoolean((String) environmentProperties.getOrDefault("testcontainers.reuse.enable", "false"));
    }

    public String getDockerClientStrategyClassName() {
        return (String) environmentProperties.get("docker.client.strategy");
    }

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
