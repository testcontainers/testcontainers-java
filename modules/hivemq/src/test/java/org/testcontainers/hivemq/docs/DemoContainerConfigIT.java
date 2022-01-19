package org.testcontainers.hivemq.docs;

import com.github.dockerjava.api.model.HostConfig;
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
import org.testcontainers.utility.MountableFile;

import java.util.concurrent.TimeUnit;

@Disabled("This is used for the docs, running the test would create several instances of HiveMQ which isn't needed for checking the code builds.")
@Testcontainers
public class DemoContainerConfigIT {

    //dockerConfig
    @Container
    final HiveMQContainer hivemqDockerConfig = new HiveMQContainer(HiveMQContainer.DEFAULT_HIVEMQ_CE_IMAGE_NAME)
        .withCreateContainerCmdModifier(createContainerCmd -> {
            final HostConfig hostConfig = HostConfig.newHostConfig();
            hostConfig.withCpuCount(2L);
            hostConfig.withMemory(2 * 1024 * 1024L);
        });
    //

    //containerConfig
    @Container
    final HiveMQContainer hivemq =
        new HiveMQContainer(HiveMQContainer.DEFAULT_HIVEMQ_EE_IMAGE_NAME)
            .withLogLevel(Level.DEBUG) //set Log level to DEBUG
            .withControlCenter() // enable the Control Center => This is an enterprise feature
            .withHiveMQConfig(MountableFile.forClasspathResource("/config.xml")); // Use a dedicated configuration
    //

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    public void test() throws Exception {

        //mqtt5client
        final Mqtt5BlockingClient client = Mqtt5Client.builder()
            .serverPort(hivemq.getMqttPort())
            .buildBlocking();

        client.connect();
        client.disconnect();
        //

    }
}
