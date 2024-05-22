package org.testcontainers.hivemq.docs;

import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.event.Level;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.concurrent.TimeUnit;

@Testcontainers
class DemoHiveMQContainerIT {

    // ceVersion {
    @Container
    final HiveMQContainer hivemqCe = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq-ce").withTag("2024.3"))
        .withLogLevel(Level.DEBUG);

    // }

    // hiveEEVersion {
    @Container
    final HiveMQContainer hivemqEe = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq4").withTag("4.7.4"))
        .withLogLevel(Level.DEBUG);

    // }

    // eeVersionWithControlCenter {
    @Container
    final HiveMQContainer hivemqEeWithControlCenter = new HiveMQContainer(
        DockerImageName.parse("hivemq/hivemq4").withTag("4.7.4")
    )
        .withLogLevel(Level.DEBUG)
        .withHiveMQConfig(MountableFile.forClasspathResource("/inMemoryConfig.xml"))
        .withControlCenter();

    // }

    // specificVersion {
    @Container
    final HiveMQContainer hivemqSpecificVersion = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq-ce:2024.3"));

    // }

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void test() throws Exception {
        // mqtt5client {
        final Mqtt5BlockingClient client = Mqtt5Client
            .builder()
            .serverPort(hivemqCe.getMqttPort())
            .serverHost(hivemqCe.getHost())
            .buildBlocking();

        client.connect();
        client.disconnect();
        // }

    }
}
