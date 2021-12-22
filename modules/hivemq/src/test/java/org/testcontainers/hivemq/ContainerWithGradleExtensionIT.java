package org.testcontainers.hivemq;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.hivemq.util.TestPublishModifiedUtil;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * @author Yannick Weber
 */
public class ContainerWithGradleExtensionIT {

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    public void test() throws Exception {
        final MountableFile gradleExtension = new GradleHiveMQExtensionSupplier(
            new File(getClass().getResource("/gradle-extension").toURI()))
            .get();

        final HiveMQContainer container = new HiveMQContainer()
            .withHiveMQConfig(MountableFile.forClasspathResource("/inMemoryConfig.xml"))
            .waitForExtension("Gradle Extension")
            .withExtension(gradleExtension);

        container.start();
        TestPublishModifiedUtil.testPublishModified(container.getMqttPort());
        container.stop();
    }
}
