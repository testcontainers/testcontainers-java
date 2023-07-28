package org.testcontainers.hivemq.docs;

import org.junit.jupiter.api.Test;
import org.testcontainers.hivemq.HiveMQContainer;
import org.testcontainers.hivemq.HiveMQExtension;
import org.testcontainers.hivemq.util.MyExtension;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

@Testcontainers
class DemoDisableExtensionsIT {

    // noExtensions {
    @Container
    final HiveMQContainer hivemqNoExtensions = new HiveMQContainer(
        DockerImageName.parse("hivemq/hivemq4").withTag("4.7.4")
    )
        .withoutPrepackagedExtensions();

    // }

    // noKafkaExtension {
    @Container
    final HiveMQContainer hivemqNoKafkaExtension = new HiveMQContainer(
        DockerImageName.parse("hivemq/hivemq4").withTag("4.7.4")
    )
        .withoutPrepackagedExtensions("hivemq-kafka-extension");

    // }

    // startDisabled {
    private final HiveMQExtension hiveMQExtension = HiveMQExtension
        .builder()
        .id("extension-1")
        .name("my-extension")
        .version("1.0")
        .disabledOnStartup(true)
        .mainClass(MyExtension.class)
        .build();

    @Container
    final HiveMQContainer hivemq = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq4").withTag("4.7.4"))
        .withExtension(hiveMQExtension);

    // }

    // startFromFilesystem {
    @Container
    final HiveMQContainer hivemqExtensionFromFilesystem = new HiveMQContainer(
        DockerImageName.parse("hivemq/hivemq4").withTag("4.7.4")
    )
        .withExtension(MountableFile.forHostPath("src/test/resources/modifier-extension"));

    // }

    // hiveRuntimeEnable {
    @Test
    void test_disable_enable_extension() throws Exception {
        hivemq.enableExtension(hiveMQExtension);
        hivemq.disableExtension(hiveMQExtension);
    }

    // }

    // runtimeEnableFilesystem {
    @Test
    void test_disable_enable_extension_from_filesystem() throws Exception {
        hivemqExtensionFromFilesystem.disableExtension("Modifier Extension", "modifier-extension");
        hivemqExtensionFromFilesystem.enableExtension("Modifier Extension", "modifier-extension");
    }
    // }
}
