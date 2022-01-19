package org.testcontainers.hivemq.docs;

import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.event.Level;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.hivemq.HiveMQExtension;
import org.testcontainers.hivemq.util.MyExtensionWithSubclasses;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.concurrent.TimeUnit;

@Disabled("This is used for the docs, running the test would create several instances of HiveMQ which isn't needed for checking the code builds.")
@Testcontainers
public class DemoHiveMQContainerIT {

    //ceVersion
    @Container
    final HiveMQContainer hivemqCe =
        new HiveMQContainer(HiveMQContainer.DEFAULT_HIVEMQ_CE_IMAGE_NAME)
            .withLogLevel(Level.DEBUG);
    //

    //eeVersion
    @Container
    final HiveMQContainer hivemqEe =
        new HiveMQContainer(HiveMQContainer.DEFAULT_HIVEMQ_CE_IMAGE_NAME)
            .withLogLevel(Level.DEBUG);
    //

    //specificVersion
    @Container
    final HiveMQContainer hivemqSpecificVersion = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq-ce:2021.3"));
    //

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    public void test() throws Exception {

        //mqtt5client
        final Mqtt5BlockingClient client = Mqtt5Client.builder()
            .serverPort(hivemqCe.getMqttPort())
            .buildBlocking();

        client.connect();
        client.disconnect();
        //

    }
}
