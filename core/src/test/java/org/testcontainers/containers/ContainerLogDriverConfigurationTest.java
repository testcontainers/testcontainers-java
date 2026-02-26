package org.testcontainers.containers;

import com.github.dockerjava.api.model.LogConfig;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.TestImages;
import org.testcontainers.utility.MockTestcontainersConfigurationExtension;
import org.testcontainers.utility.TestcontainersConfiguration;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

@ExtendWith(MockTestcontainersConfigurationExtension.class)
class ContainerLogDriverConfigurationTest {

    private static String daemonDefault;

    @BeforeAll
    static void fetchDaemonDefault() {
        daemonDefault = DockerClientFactory.instance().client().infoCmd().exec().getLoggingDriver();
    }

    @Test
    void shouldApplyConfiguredLogDriverToContainer() {
        LogConfig.LoggingType overrideDriver = daemonDefault.equals(LogConfig.LoggingType.NONE.getType())
            ? LogConfig.LoggingType.JSON_FILE
            : LogConfig.LoggingType.NONE;

        Mockito
            .doReturn(Optional.of(overrideDriver.getType()))
            .when(TestcontainersConfiguration.getInstance())
            .getContainerLogDriver();

        try (
            GenericContainer<?> container = new GenericContainer<>(TestImages.TINY_IMAGE)
                .withCommand("tail", "-f", "/dev/null")
        ) {
            container.start();

            assertThat(container.getContainerInfo().getHostConfig().getLogConfig().getType().getType())
                .as("container should use the configured log driver instead of the daemon default (%s)", daemonDefault)
                .isEqualTo(overrideDriver.getType());
        }
    }

    @Test
    void shouldNotOverrideLogDriverWhenNotConfigured() {
        Mockito.doReturn(Optional.empty()).when(TestcontainersConfiguration.getInstance()).getContainerLogDriver();

        try (
            GenericContainer<?> container = new GenericContainer<>(TestImages.TINY_IMAGE)
                .withCommand("tail", "-f", "/dev/null")
        ) {
            container.start();

            assertThat(container.getContainerInfo().getHostConfig().getLogConfig().getType().getType())
                .as("container should use the daemon default log driver when none is configured")
                .isEqualTo(daemonDefault);
        }
    }

    @Test
    void shouldAllowPerContainerOverrideOfGlobalLogDriver() {
        // Set global config to the non-default driver
        LogConfig.LoggingType globalDriver = daemonDefault.equals(LogConfig.LoggingType.NONE.getType())
            ? LogConfig.LoggingType.JSON_FILE
            : LogConfig.LoggingType.NONE;
        Mockito
            .doReturn(Optional.of(globalDriver.getType()))
            .when(TestcontainersConfiguration.getInstance())
            .getContainerLogDriver();

        // Pick a different driver to override with at the per-container level
        LogConfig.LoggingType perContainerDriver = globalDriver == LogConfig.LoggingType.NONE
            ? LogConfig.LoggingType.JSON_FILE
            : LogConfig.LoggingType.NONE;

        try (
            GenericContainer<?> container = new GenericContainer<>(TestImages.TINY_IMAGE)
                .withCreateContainerCmdModifier(cmd -> {
                    cmd.getHostConfig().withLogConfig(new LogConfig(perContainerDriver));
                })
                .withCommand("tail", "-f", "/dev/null")
        ) {
            container.start();

            assertThat(container.getContainerInfo().getHostConfig().getLogConfig().getType().getType())
                .as("per-container modifier should override the global container.log.driver setting")
                .isNotEqualTo(globalDriver.getType())
                .isEqualTo(perContainerDriver.getType());
        }
    }

    @Test
    void shouldWarnAndIgnoreUnsupportedLogDriver() {
        Mockito
            .doReturn(Optional.of("invalid-driver-xyz"))
            .when(TestcontainersConfiguration.getInstance())
            .getContainerLogDriver();

        try (
            GenericContainer<?> container = new GenericContainer<>(TestImages.TINY_IMAGE)
                .withCommand("tail", "-f", "/dev/null")
        ) {
            assertThatNoException().isThrownBy(container::start);

            assertThat(container.getContainerInfo().getHostConfig().getLogConfig().getType().getType())
                .as("container should fall back to the daemon default log driver when an invalid driver is configured")
                .isEqualTo(daemonDefault);
        }
    }
}
