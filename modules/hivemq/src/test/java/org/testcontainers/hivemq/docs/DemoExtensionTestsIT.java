package org.testcontainers.hivemq.docs;

import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.hivemq.HiveMQExtension;
import org.testcontainers.hivemq.util.MyExtensionWithSubclasses;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.util.concurrent.TimeUnit;

@Disabled("This is used for the docs, running the test would create several instances of HiveMQ which isn't needed for checking the code builds.")
@Testcontainers
public class DemoExtensionTestsIT {

    //waitStrategy
    @Container
    final HiveMQContainer hivemqWithWaitStrategy =
        new HiveMQContainer(HiveMQContainer.DEFAULT_HIVEMQ_EE_IMAGE_NAME)
            .withExtension(MountableFile.forClasspathResource("/modifier-extension"))
            .withDebugging() // enable debugging
            .waitForExtension("Modifier Extension");
    //

    //extensionClasspath
    final HiveMQExtension hiveMQEClasspathxtension = HiveMQExtension.builder()
        .id("extension-1")
        .name("my-extension")
        .version("1.0")
        .mainClass(MyExtensionWithSubclasses.class).build();

    @Container
    final HiveMQContainer hivemqWithClasspathExtension =
        new HiveMQContainer(HiveMQContainer.DEFAULT_HIVEMQ_CE_IMAGE_NAME)
            .waitForExtension(hiveMQEClasspathxtension)
            .withExtension(hiveMQEClasspathxtension)
            .withDebugging() // enable debugging
            .withHiveMQConfig(MountableFile.forClasspathResource("/inMemoryConfig.xml"));
    //

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    public void test() throws Exception {

        //mqtt5client
        final Mqtt5BlockingClient client = Mqtt5Client.builder()
            .serverPort(hivemqWithClasspathExtension.getMqttPort())
            .buildBlocking();

        client.connect();
        client.disconnect();
        //

    }
}
