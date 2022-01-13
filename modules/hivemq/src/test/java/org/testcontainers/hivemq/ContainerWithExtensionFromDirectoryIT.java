package org.testcontainers.hivemq;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.event.Level;
import org.testcontainers.hivemq.util.TestPublishModifiedUtil;
import org.testcontainers.utility.MountableFile;

import java.util.concurrent.TimeUnit;

public class ContainerWithExtensionFromDirectoryIT {

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void test() throws Exception {
        try (
            final HiveMQContainer hivemq =
                new HiveMQContainer(HiveMQContainer.DEFAULT_HIVEMQ_CE_IMAGE_NAME)
                    .withExtension(MountableFile.forClasspathResource("/modifier-extension"))
                    .waitForExtension("Modifier Extension")
                    .withHiveMQConfig(MountableFile.forClasspathResource("/inMemoryConfig.xml"))
                    .withLogLevel(Level.DEBUG)) {

            hivemq.start();
            TestPublishModifiedUtil.testPublishModified(hivemq.getMqttPort());
            hivemq.stop();
        }
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void test_wrongDirectoryName() throws Exception {
        try (final HiveMQContainer hivemq =
            new HiveMQContainer(HiveMQContainer.DEFAULT_HIVEMQ_CE_IMAGE_NAME)
                .withExtension(MountableFile.forClasspathResource("/modifier-extension-wrong-name"))
                .waitForExtension("Modifier Extension")
                .withLogLevel(Level.DEBUG)) {

            hivemq.start();
            TestPublishModifiedUtil.testPublishModified(hivemq.getMqttPort());
            hivemq.stop();
        }
    }
}
