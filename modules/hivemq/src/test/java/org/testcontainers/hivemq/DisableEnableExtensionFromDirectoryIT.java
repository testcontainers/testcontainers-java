package org.testcontainers.hivemq;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.event.Level;
import org.testcontainers.hivemq.util.TestPublishModifiedUtil;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class DisableEnableExtensionFromDirectoryIT {

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void test() throws Exception {
        try (
            final HiveMQContainer hivemq = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq4").withTag("4.7.4"))
                .withExtension(MountableFile.forClasspathResource("/modifier-extension"))
                .waitForExtension("Modifier Extension")
                .withLogLevel(Level.DEBUG)
        ) {
            hivemq.start();

            TestPublishModifiedUtil.testPublishModified(hivemq.getMqttPort(), hivemq.getHost());
            hivemq.disableExtension("Modifier Extension", "modifier-extension");
            assertThatExceptionOfType(ExecutionException.class)
                .isThrownBy(() -> TestPublishModifiedUtil.testPublishModified(hivemq.getMqttPort(), hivemq.getHost()));
            hivemq.enableExtension("Modifier Extension", "modifier-extension");
            TestPublishModifiedUtil.testPublishModified(hivemq.getMqttPort(), hivemq.getHost());
        }
    }
}
