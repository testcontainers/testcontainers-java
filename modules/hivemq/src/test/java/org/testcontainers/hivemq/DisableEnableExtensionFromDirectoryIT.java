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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.slf4j.event.Level;
import org.testcontainers.hivemq.util.TestPublishModifiedUtil;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * @author Yannick Weber
 */
public class DisableEnableExtensionFromDirectoryIT {

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void test() throws Exception {
        final HiveMQContainer extension =
                new HiveMQContainer(DockerImageName.parse("hivemq/hivemq4").withTag("latest"))
                        .withExtension(MountableFile.forClasspathResource("/modifier-extension"))
                        .waitForExtension("Modifier Extension")
                        .withLogLevel(Level.DEBUG);

        extension.start();

        TestPublishModifiedUtil.testPublishModified(extension.getMqttPort());
        extension.disableExtension("Modifier Extension", "modifier-extension");
        assertThrows(ExecutionException.class, () -> TestPublishModifiedUtil.testPublishModified(extension.getMqttPort()));
        extension.enableExtension("Modifier Extension", "modifier-extension");
        TestPublishModifiedUtil.testPublishModified(extension.getMqttPort());

        extension.stop();
    }

}
