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

        final HiveMQContainer container = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq4").withTag("latest"))
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

        final HiveMQContainer container = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq4").withTag("latest"))
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
