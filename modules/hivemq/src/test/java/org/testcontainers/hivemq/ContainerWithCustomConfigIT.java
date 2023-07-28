package org.testcontainers.hivemq;

import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.exceptions.MqttSessionExpiredException;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class ContainerWithCustomConfigIT {

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void test() throws Exception {
        try (
            final HiveMQContainer hivemq = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq4").withTag("4.7.4"))
                .withHiveMQConfig(MountableFile.forClasspathResource("/config.xml"))
        ) {
            hivemq.start();

            final Mqtt5BlockingClient publisher = Mqtt5Client
                .builder()
                .identifier("publisher")
                .serverPort(hivemq.getMqttPort())
                .serverHost(hivemq.getHost())
                .buildBlocking();

            publisher.connect();

            assertThatExceptionOfType(MqttSessionExpiredException.class)
                .isThrownBy(() -> {
                    // this should fail since only QoS 0 is allowed by the configuration
                    publisher.publishWith().topic("test/topic").qos(MqttQos.EXACTLY_ONCE).send();
                });
        }
    }
}
