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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Disabled("This is used for the docs, running the test would create several instances of HiveMQ which isn't needed for checking the code builds.")
@Testcontainers
public class DemoDisableExtensionsIT {

    //noExtensions
    @Container
    final HiveMQContainer hivemqNoExtensions = new HiveMQContainer(HiveMQContainer.DEFAULT_HIVEMQ_EE_IMAGE_NAME)
        .withoutPrepackagedExtensions();
    //

    //noKafkaExtension
    @Container
    final HiveMQContainer hivemqNoKafkaExtension = new HiveMQContainer(HiveMQContainer.DEFAULT_HIVEMQ_EE_IMAGE_NAME)
        .withoutPrepackagedExtensions("hivemq-kafka-extension");
    //

    //startDisabled
    private final HiveMQExtension hiveMQExtension = HiveMQExtension.builder()
        .id("extension-1")
        .name("my-extension")
        .version("1.0")
        .disabledOnStartup(true)
        .mainClass(MyExtension.class).build();

    @Container
    final HiveMQContainer hivemq = new HiveMQContainer(HiveMQContainer.DEFAULT_HIVEMQ_EE_IMAGE_NAME)
        .withExtension(hiveMQExtension);
    //


    //startFromFilesystem
    @Container
    final HiveMQContainer hivemqExtensionFromFilesystem = new HiveMQContainer(HiveMQContainer.DEFAULT_HIVEMQ_EE_IMAGE_NAME)
        .withExtension(MountableFile.forHostPath("src/test/resources/modifier-extension"));
    //


    //runtimeEnable
    @Test
    void test_disable_enable_extension() throws Exception {
        hivemq.enableExtension(hiveMQExtension);
        hivemq.disableExtension(hiveMQExtension);
    }
    //

    //runtimeEnableFilesystem
    @Test
    void test_disable_enable_extension_from_filesystem() throws Exception {
        hivemqExtensionFromFilesystem.disableExtension("Modifier Extension", "modifier-extension");
        hivemqExtensionFromFilesystem.enableExtension("Modifier Extension", "modifier-extension");
    }
    //
}
