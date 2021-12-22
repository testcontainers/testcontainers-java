package org.testcontainers.hivemq;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.hivemq.util.TestPublishModifiedUtil;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Yannick Weber
 */
public class DisableEnableMavenExtensionIT {

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void test() throws Exception {
        final MountableFile extensionDir = new MavenHiveMQExtensionSupplier(
                getClass().getResource("/maven-extension/pom.xml").getPath())
                .addProperty("HIVEMQ_GROUP_ID", "com.hivemq")
                .addProperty("HIVEMQ_EXTENSION_SDK", "hivemq-extension-sdk")
                .addProperty("HIVEMQ_EXTENSION_SDK_VERSION", "4.3.0")
                .quiet()
                .get();

        final HiveMQContainer extension =
                new HiveMQContainer(HiveMQContainer.DEFAULT_HIVEMQ_EE_IMAGE_NAME)
                        .waitForExtension("Maven Extension")
                        .withExtension(extensionDir);

        extension.start();
        TestPublishModifiedUtil.testPublishModified(extension.getMqttPort());
        extension.disableExtension("Maven Extension", "maven-extension");
        assertThrows(ExecutionException.class, () -> TestPublishModifiedUtil.testPublishModified(extension.getMqttPort()));
        extension.enableExtension("Maven Extension", "maven-extension");
        TestPublishModifiedUtil.testPublishModified(extension.getMqttPort());
        extension.stop();
    }

}
