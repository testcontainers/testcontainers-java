package org.testcontainers.hivemq;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.event.Level;
import org.testcontainers.hivemq.util.TestPublishModifiedUtil;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.concurrent.TimeUnit;

class ContainerWithExtensionFromDirectoryIT {

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void test() throws Exception {
        try (
            final HiveMQContainer hivemq = new HiveMQContainer(
                DockerImageName.parse("hivemq/hivemq-ce").withTag("2021.3")
            )
                .withExtension(MountableFile.forClasspathResource("/modifier-extension"))
                .waitForExtension("Modifier Extension")
                .withHiveMQConfig(MountableFile.forClasspathResource("/inMemoryConfig.xml"))
                .withLogLevel(Level.DEBUG)
        ) {
            hivemq.start();
            TestPublishModifiedUtil.testPublishModified(hivemq.getMqttPort(), hivemq.getHost());
        }
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void test_wrongDirectoryName() throws Exception {
        try (
            final HiveMQContainer hivemq = new HiveMQContainer(
                DockerImageName.parse("hivemq/hivemq-ce").withTag("2021.3")
            )
                .withExtension(MountableFile.forClasspathResource("/modifier-extension-wrong-name"))
                .waitForExtension("Modifier Extension")
                .withLogLevel(Level.DEBUG)
        ) {
            hivemq.start();
            TestPublishModifiedUtil.testPublishModified(hivemq.getMqttPort(), hivemq.getHost());
        }
    }
}
