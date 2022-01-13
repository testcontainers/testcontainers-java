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

import static org.junit.jupiter.api.Assertions.assertThrows;

public class ContainerWithCustomConfigIT {

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void test() throws Exception {
        final HiveMQContainer hivemq = new HiveMQContainer(HiveMQContainer.DEFAULT_HIVEMQ_EE_IMAGE_NAME)
            .withHiveMQConfig(MountableFile.forClasspathResource("/config.xml"));

        hivemq.start();

        final Mqtt5BlockingClient publisher = Mqtt5Client.builder()
            .identifier("publisher")
            .serverPort(hivemq.getMqttPort())
            .buildBlocking();

        publisher.connect();

        assertThrows(MqttSessionExpiredException.class, () -> {
            // this should fail since only QoS 0 is allowed by the configuration
            publisher.publishWith()
                .topic("test/topic")
                .qos(MqttQos.EXACTLY_ONCE)
                .send();
        });

        hivemq.stop();
    }
}
