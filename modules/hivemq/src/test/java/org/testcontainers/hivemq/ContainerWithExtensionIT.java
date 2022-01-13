package org.testcontainers.hivemq;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.hivemq.util.MyExtension;
import org.testcontainers.hivemq.util.TestPublishModifiedUtil;
import org.testcontainers.utility.MountableFile;

import java.util.concurrent.TimeUnit;

public class ContainerWithExtensionIT {

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void test() throws Exception {
        final HiveMQExtension hiveMQExtension = HiveMQExtension.builder()
            .id("extension-1")
            .name("my-extension")
            .version("1.0")
            .mainClass(MyExtension.class).build();

        final HiveMQContainer hivemq =
            new HiveMQContainer(HiveMQContainer.DEFAULT_HIVEMQ_CE_IMAGE_NAME)
                .withHiveMQConfig(MountableFile.forClasspathResource("/inMemoryConfig.xml"))
                .waitForExtension(hiveMQExtension)
                .withExtension(hiveMQExtension);

        hivemq.start();
        TestPublishModifiedUtil.testPublishModified(hivemq.getMqttPort());
        hivemq.stop();

        hivemq.start();
        TestPublishModifiedUtil.testPublishModified(hivemq.getMqttPort());
        hivemq.stop();

        hivemq.start();
        TestPublishModifiedUtil.testPublishModified(hivemq.getMqttPort());
        hivemq.stop();
    }
}
