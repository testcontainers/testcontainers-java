/*
 * Copyright 2020 HiveMQ and the HiveMQ Community
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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

/**
 * @author Yannick Weber
 */
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
        final HiveMQContainer extension =
                new HiveMQContainer(DockerImageName.parse("hivemq/hivemq4").withTag("latest"))
                        .withExtension(hiveMQExtension)
                        .withLogLevel(Level.DEBUG);

        extension.start();

        assertThrows(ExecutionException.class, () -> TestPublishModifiedUtil.testPublishModified(extension.getMqttPort()));
        extension.enableExtension(hiveMQExtension);
        TestPublishModifiedUtil.testPublishModified(extension.getMqttPort());
        extension.disableExtension(hiveMQExtension);
        assertThrows(ExecutionException.class, () -> TestPublishModifiedUtil.testPublishModified(extension.getMqttPort()));
        extension.enableExtension(hiveMQExtension);
        TestPublishModifiedUtil.testPublishModified(extension.getMqttPort());

        extension.stop();
    }

}
