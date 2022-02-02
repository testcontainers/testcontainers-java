package org.testcontainers.hivemq.docs;

import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5Client;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.event.Level;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.hivemq.HiveMQExtension;
import org.testcontainers.hivemq.util.MyExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.util.concurrent.TimeUnit;

@Disabled("This test is only used for documentation and would cause extremely high load on the CI-server if run. The test is not required to verify that the code builds.")
@Testcontainers
public class DemoFilesIT {

    // hivemqHome {
    final HiveMQContainer hivemqFileInHome = new HiveMQContainer(HiveMQContainer.DEFAULT_HIVEMQ_CE_IMAGE_NAME)
        .withFileInHomeFolder(
            MountableFile.forHostPath("src/test/resources/additionalFile.txt"),
            "/path/in/home/folder");
    // }

    // extensionHome {
    @Container
    final HiveMQContainer hivemqFileInExtensionHome = new HiveMQContainer(HiveMQContainer.DEFAULT_HIVEMQ_CE_IMAGE_NAME)
        .withExtension(HiveMQExtension.builder()
            .id("extension-1")
            .name("my-extension")
            .version("1.0")
            .mainClass(MyExtension.class).build())
        .withFileInExtensionHomeFolder(
            MountableFile.forHostPath("src/test/resources/additionalFile.txt"),
            "extension-1",
            "/path/in/extension/home");
    // }

    // withLicenses {
    @Container
    final HiveMQContainer hivemq = new HiveMQContainer(HiveMQContainer.DEFAULT_HIVEMQ_CE_IMAGE_NAME)
        .withLicense(MountableFile.forHostPath("src/test/resources/myLicense.lic"))
        .withLicense(MountableFile.forHostPath("src/test/resources/myExtensionLicense.elic"));
    // }

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    public void test() throws Exception {

        // mqtt5client {
        final Mqtt5BlockingClient client = Mqtt5Client.builder()
            .serverPort(hivemq.getMqttPort())
            .buildBlocking();

        client.connect();
        client.disconnect();
        // }

    }
}
