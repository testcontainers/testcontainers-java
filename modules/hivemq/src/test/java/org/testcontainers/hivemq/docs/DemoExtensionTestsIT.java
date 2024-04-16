package org.testcontainers.hivemq.docs;

import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.hivemq.HiveMQExtension;
import org.testcontainers.hivemq.util.MyExtensionWithSubclasses;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.concurrent.TimeUnit;

@Testcontainers
class DemoExtensionTestsIT {

    // waitStrategy {
    @Container
    final HiveMQContainer hivemqWithWaitStrategy = new HiveMQContainer(
        DockerImageName.parse("hivemq/hivemq4").withTag("4.7.4")
    )
        .withExtension(MountableFile.forClasspathResource("/modifier-extension"))
        .waitForExtension("Modifier Extension");

    // }

    // extensionClasspath {
    final HiveMQExtension hiveMQEClasspathxtension = HiveMQExtension
        .builder()
        .id("extension-1")
        .name("my-extension")
        .version("1.0")
        .mainClass(MyExtensionWithSubclasses.class)
        .build();

    @Container
    final HiveMQContainer hivemqWithClasspathExtension = new HiveMQContainer(
        DockerImageName.parse("hivemq/hivemq-ce").withTag("2024.3")
    )
        .waitForExtension(hiveMQEClasspathxtension)
        .withExtension(hiveMQEClasspathxtension)
        .withHiveMQConfig(MountableFile.forClasspathResource("/inMemoryConfig.xml"));

    // }

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void test() throws Exception {
        // mqtt5client {
        final Mqtt5BlockingClient client = Mqtt5Client
            .builder()
            .serverPort(hivemqWithClasspathExtension.getMqttPort())
            .serverHost(hivemqWithClasspathExtension.getHost())
            .buildBlocking();

        client.connect();
        client.disconnect();
        // }

    }
}
