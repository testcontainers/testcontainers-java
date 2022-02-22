package org.testcontainers.hivemq;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.event.Level;
import org.testcontainers.hivemq.util.MyExtension;
import org.testcontainers.hivemq.util.TestPublishModifiedUtil;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class DisableEnableExtensionIT {

    private final @NotNull HiveMQExtension hiveMQExtension = HiveMQExtension.builder()
        .id("extension-1")
        .name("my-extension")
        .version("1.0")
        .disabledOnStartup(true)
        .mainClass(MyExtension.class).build();

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void test() throws Exception {
        try (final HiveMQContainer hivemq =
                 new HiveMQContainer(DockerImageName.parse("hivemq/hivemq4").withTag("4.7.4"))
                     .withExtension(hiveMQExtension)
                     .withLogLevel(Level.DEBUG)) {


            hivemq.start();

            assertThrows(ExecutionException.class, () -> TestPublishModifiedUtil.testPublishModified(hivemq.getMqttPort(), hivemq.getHost()));
            hivemq.enableExtension(hiveMQExtension);
            TestPublishModifiedUtil.testPublishModified(hivemq.getMqttPort(), hivemq.getHost());
            hivemq.disableExtension(hiveMQExtension);
            assertThrows(ExecutionException.class, () -> TestPublishModifiedUtil.testPublishModified(hivemq.getMqttPort(), hivemq.getHost()));
            hivemq.enableExtension(hiveMQExtension);
            TestPublishModifiedUtil.testPublishModified(hivemq.getMqttPort(), hivemq.getHost());
        }
    }

}
