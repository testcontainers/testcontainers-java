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

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.extension.sdk.api.ExtensionMain;
import com.hivemq.extension.sdk.api.auth.SimpleAuthenticator;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStartOutput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopInput;
import com.hivemq.extension.sdk.api.parameter.ExtensionStopOutput;
import com.hivemq.extension.sdk.api.services.Services;
import com.hivemq.extension.sdk.api.services.builder.Builders;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.testcontainers.utility.DockerImageName;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Yannick Weber
 */
public class ContainerWithoutPlatformExtensionsIT {

    private final @NotNull HiveMQExtension hiveMQExtension = HiveMQExtension.builder()
            .name("MyExtension")
            .id("my-extension")
            .version("1.0.0")
            .mainClass(CheckerExtension.class).build();


    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    public void removeAllPlatformExtensions() throws InterruptedException {

        final HiveMQContainer container = new HiveMQContainer(HiveMQContainer.DEFAULT_HIVEMQ_EE_IMAGE_NAME.withTag(HiveMQContainer.DEFAULT_HIVEMQ_EE_TAG))
                .withExtension(hiveMQExtension)
                .waitForExtension(hiveMQExtension)
                .withoutPrepackagedExtensions();

        container.start();

        final Mqtt5BlockingClient client = MqttClient.builder()
                .serverPort(container.getMqttPort())
                .useMqttVersion5()
                .buildBlocking();

        client.connect();
        final Mqtt5BlockingClient.Mqtt5Publishes publishes = client.publishes(MqttGlobalPublishFilter.ALL);
        client.subscribeWith().topicFilter("extensions").send();

        final Mqtt5Publish receive = publishes.receive();
        assertTrue(receive.getPayload().isPresent());
        final String extensionInfo = new String(receive.getPayloadAsBytes());

        assertFalse(extensionInfo.contains("hivemq-allow-all-extension"));
        assertFalse(extensionInfo.contains("hivemq-kafka-extension"));
        assertFalse(extensionInfo.contains("hivemq-bridge-extension"));
        assertFalse(extensionInfo.contains("hivemq-enterprise-security-extension"));

        container.start();
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    public void removeKafkaExtension() throws InterruptedException {

        final HiveMQContainer container = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq4").withTag(HiveMQContainer.DEFAULT_HIVEMQ_EE_TAG))
                .withExtension(hiveMQExtension)
                .waitForExtension(hiveMQExtension)
                .withoutPrepackagedExtensions("hivemq-kafka-extension");

        container.start();

        final Mqtt5BlockingClient client = MqttClient.builder()
                .serverPort(container.getMqttPort())
                .useMqttVersion5()
                .buildBlocking();

        client.connect();
        final Mqtt5BlockingClient.Mqtt5Publishes publishes = client.publishes(MqttGlobalPublishFilter.ALL);
        client.subscribeWith().topicFilter("extensions").send();

        final Mqtt5Publish receive = publishes.receive();
        assertTrue(receive.getPayload().isPresent());
        final String extensionInfo = new String(receive.getPayloadAsBytes());

        assertTrue(extensionInfo.contains("hivemq-allow-all-extension"));
        assertFalse(extensionInfo.contains("hivemq-kafka-extension"));
        assertTrue(extensionInfo.contains("hivemq-bridge-extension"));
        assertTrue(extensionInfo.contains("hivemq-enterprise-security-extension"));

        container.stop();
    }

    public static class CheckerExtension implements ExtensionMain {

        @Override
        public void extensionStart(
                final @NotNull ExtensionStartInput extensionStartInput,
                final @NotNull ExtensionStartOutput extensionStartOutput) {

            final String extensionFolders = Arrays.stream(extensionStartInput.getServerInformation().getExtensionsFolder().listFiles())
                    .filter(File::isDirectory)
                    .map(File::getName)
                    .collect(Collectors.joining("\n"));

            final byte[] bytes = extensionFolders.getBytes(StandardCharsets.UTF_8);
            Services.publishService().publish(Builders.publish()
                    .topic("extensions")
                    .retain(true)
                    .payload(ByteBuffer.wrap(bytes))
                    .build());

            Services.securityRegistry().setAuthenticatorProvider(authenticatorProviderInput ->
                    (SimpleAuthenticator) (simpleAuthInput, simpleAuthOutput) ->
                            simpleAuthOutput.authenticateSuccessfully());

        }

        @Override
        public void extensionStop(
                final @NotNull ExtensionStopInput extensionStopInput,
                final @NotNull ExtensionStopOutput extensionStopOutput) {

        }
    }
}
