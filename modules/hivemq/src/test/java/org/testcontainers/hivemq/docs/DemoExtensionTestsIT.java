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
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.concurrent.TimeUnit;

@Disabled("This test is only used for documentation and would cause extremely high load on the CI-server if run. The test is not required to verify that the code builds.")
@Testcontainers
public class DemoExtensionTestsIT {

    // waitStrategy {
    @Container
    final HiveMQContainer hivemqWithWaitStrategy =
        new HiveMQContainer(DockerImageName.parse("hivemq/hivemq4").withTag("4.7.4"))
            .withExtension(MountableFile.forClasspathResource("/modifier-extension"))
            .withDebugging() // enable debugging
            .waitForExtension("Modifier Extension");
    // }

    // extensionClasspath {
    final HiveMQExtension hiveMQEClasspathxtension = HiveMQExtension.builder()
        .id("extension-1")
        .name("my-extension")
        .version("1.0")
        .mainClass(MyExtensionWithSubclasses.class).build();

    @Container
    final HiveMQContainer hivemqWithClasspathExtension =
        new HiveMQContainer(DockerImageName.parse("hivemq/hivemq-ce").withTag("2021.3"))
            .waitForExtension(hiveMQEClasspathxtension)
            .withExtension(hiveMQEClasspathxtension)
            .withDebugging() // enable debugging
            .withHiveMQConfig(MountableFile.forClasspathResource("/inMemoryConfig.xml"));
    // }

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    public void test() throws Exception {

        // mqtt5client {
        final Mqtt5BlockingClient client = Mqtt5Client.builder()
            .serverPort(hivemqWithClasspathExtension.getMqttPort())
            .buildBlocking();

        client.connect();
        client.disconnect();
        // }

    }
}
