package org.testcontainers.hivemq;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.event.Level;
import org.testcontainers.hivemq.util.MyExtensionWithSubclasses;
import org.testcontainers.hivemq.util.TestPublishModifiedUtil;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.concurrent.TimeUnit;

class ContainerWithExtensionSubclassIT {

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void test() throws Exception {
        final HiveMQExtension hiveMQExtension = HiveMQExtension
            .builder()
            .id("extension-1")
            .name("my-extension")
            .version("1.0")
            .mainClass(MyExtensionWithSubclasses.class)
            .build();

        try (
            final HiveMQContainer hivemq = new HiveMQContainer(
                DockerImageName.parse("hivemq/hivemq-ce").withTag("2024.3")
            )
                .waitForExtension(hiveMQExtension)
                .withExtension(hiveMQExtension)
                .withHiveMQConfig(MountableFile.forClasspathResource("/inMemoryConfig.xml"))
                .withLogLevel(Level.DEBUG)
        ) {
            hivemq.start();
            TestPublishModifiedUtil.testPublishModified(hivemq.getMqttPort(), hivemq.getHost());
        }
    }
}
