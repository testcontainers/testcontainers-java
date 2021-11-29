/*
 * MIT License
 *
 * Copyright (c) 2021-present HiveMQ GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
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
                new HiveMQContainer(HiveMQContainer.DEFAULT_HIVEMQ_EE_IMAGE_NAME.withTag(HiveMQContainer.DEFAULT_HIVEMQ_EE_TAG))
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
