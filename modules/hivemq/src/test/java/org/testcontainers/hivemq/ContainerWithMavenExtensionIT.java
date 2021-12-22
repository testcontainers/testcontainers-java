package org.testcontainers.hivemq;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.hivemq.util.TestPublishModifiedUtil;
import org.testcontainers.utility.MountableFile;

import java.util.concurrent.TimeUnit;

/**
 * @author Yannick Weber
 */
public class ContainerWithMavenExtensionIT {

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void test() throws Exception {
        final MountableFile mavenExtension = new MavenHiveMQExtensionSupplier(
            getClass().getResource("/maven-extension/pom.xml").getPath())
            .addProperty("HIVEMQ_GROUP_ID", "com.hivemq")
            .addProperty("HIVEMQ_EXTENSION_SDK", "hivemq-extension-sdk")
            .addProperty("HIVEMQ_EXTENSION_SDK_VERSION", "4.3.0")
            .get();

        final HiveMQContainer extension = new HiveMQContainer()
            .withHiveMQConfig(MountableFile.forClasspathResource("/inMemoryConfig.xml"))
            .waitForExtension("Maven Extension")
            .withExtension(mavenExtension);

        extension.start();
        TestPublishModifiedUtil.testPublishModified(extension.getMqttPort());
        extension.stop();
    }

}
