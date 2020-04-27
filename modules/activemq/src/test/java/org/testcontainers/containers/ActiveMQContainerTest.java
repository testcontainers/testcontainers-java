package org.testcontainers.containers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.testcontainers.utility.MountableFile;

/**
 * @author Martin Greber
 */
public class ActiveMQContainerTest {

    public static final String DEFAULT_IMAGE = "rmohr/activemq:5.15.9-alpine";
    public static final int DEFAULT_STOMP_PORT = 61613;
    public static final int DEFAULT_WS_PORT = 61614;
    public static final int DEFAULT_JMS_PORT = 61616;
    public static final int DEFAULT_MQTT_PORT = 1883;
    public static final int DEFAULT_AMQP_PORT = 5672;
    public static final int DEFAULT_UI_PORT = 8161;

    @Test
    public void shouldCreateActiveMQContainer() {
        try (ActiveMQContainer container = new ActiveMQContainer()) {
            assertThat(container.getDockerImageName()).isEqualTo(DEFAULT_IMAGE);

            container.start();

            assertThat(container.getStompUrl()).startsWith(
                String.format("stomp://%s:%d", container.getContainerIpAddress(),
                    container.getMappedPort(DEFAULT_STOMP_PORT)));
            assertThat(container.getWsUrl()).startsWith(
                String.format("ws://%s:%d", container.getContainerIpAddress(),
                    container.getMappedPort(DEFAULT_WS_PORT)));
            assertThat(container.getJmsUrl()).startsWith(
                String.format("tcp://%s:%d", container.getContainerIpAddress(),
                    container.getMappedPort(DEFAULT_JMS_PORT)));
            assertThat(container.getMqttUrl()).startsWith(
                String.format("mqtt://%s:%d", container.getContainerIpAddress(),
                    container.getMappedPort(DEFAULT_MQTT_PORT)));
            assertThat(container.getAmqpUrl()).startsWith(
                String.format("amqp://%s:%d", container.getContainerIpAddress(),
                    container.getMappedPort(DEFAULT_AMQP_PORT)));
            assertThat(container.getUiUrl()).startsWith(
                String.format("http://%s:%d", container.getContainerIpAddress(),
                    container.getMappedPort(DEFAULT_UI_PORT)));

            assertThat(container.getStompPort())
                .isEqualTo(container.getMappedPort(DEFAULT_STOMP_PORT));
            assertThat(container.getWsPort()).isEqualTo(container.getMappedPort(DEFAULT_WS_PORT));
            assertThat(container.getJmsPort()).isEqualTo(container.getMappedPort(DEFAULT_JMS_PORT));
            assertThat(container.getMqttPort())
                .isEqualTo(container.getMappedPort(DEFAULT_MQTT_PORT));
            assertThat(container.getAmqpPort())
                .isEqualTo(container.getMappedPort(DEFAULT_AMQP_PORT));
            assertThat(container.getUiPort()).isEqualTo(container.getMappedPort(DEFAULT_UI_PORT));

            assertThat(container.getLivenessCheckPortNumbers()).containsExactlyInAnyOrder(
                container.getMappedPort(DEFAULT_STOMP_PORT),
                container.getMappedPort(DEFAULT_WS_PORT),
                container.getMappedPort(DEFAULT_JMS_PORT),
                container.getMappedPort(DEFAULT_MQTT_PORT),
                container.getMappedPort(DEFAULT_AMQP_PORT),
                container.getMappedPort(DEFAULT_UI_PORT)
            );
        }
    }

    @Test
    public void shouldCreateActiveMQContainerWithTag() {
        try (ActiveMQContainer container = new ActiveMQContainer(DEFAULT_IMAGE)) {
            assertThat(container.getDockerImageName()).isEqualTo(DEFAULT_IMAGE);
        }
    }

    @Test
    public void shouldMountConfigurationFile() {
        try (ActiveMQContainer container = new ActiveMQContainer()) {

            container.withActiveMQConfigFile(MountableFile.forClasspathResource("/activemq.xml"));
            container.start();

            assertThat(container.isRunning());
        }
    }
}
