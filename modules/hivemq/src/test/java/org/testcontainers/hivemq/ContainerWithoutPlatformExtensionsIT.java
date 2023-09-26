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

import static org.assertj.core.api.Assertions.assertThat;

class ContainerWithoutPlatformExtensionsIT {

    @NotNull
    private final HiveMQExtension hiveMQExtension = HiveMQExtension
        .builder()
        .name("MyExtension")
        .id("my-extension")
        .version("1.0.0")
        .mainClass(CheckerExtension.class)
        .build();

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void removeAllPlatformExtensions() throws InterruptedException {
        try (
            final HiveMQContainer hivemq = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq4").withTag("4.7.4"))
                .withExtension(hiveMQExtension)
                .waitForExtension(hiveMQExtension)
                .withoutPrepackagedExtensions()
        ) {
            hivemq.start();

            final Mqtt5BlockingClient client = MqttClient
                .builder()
                .serverPort(hivemq.getMqttPort())
                .serverHost(hivemq.getHost())
                .useMqttVersion5()
                .buildBlocking();

            client.connect();
            final Mqtt5BlockingClient.Mqtt5Publishes publishes = client.publishes(MqttGlobalPublishFilter.ALL);
            client.subscribeWith().topicFilter("extensions").send();

            final Mqtt5Publish receive = publishes.receive();
            assertThat(receive.getPayload()).isPresent();
            final String extensionInfo = new String(receive.getPayloadAsBytes());

            assertThat(extensionInfo).doesNotContain("hivemq-allow-all-extension");
            assertThat(extensionInfo).doesNotContain("hivemq-kafka-extension");
            assertThat(extensionInfo).doesNotContain("hivemq-bridge-extension");
            assertThat(extensionInfo).doesNotContain("hivemq-enterprise-security-extension");

            hivemq.start();
        }
    }

    @Test
    @Timeout(value = 3, unit = TimeUnit.MINUTES)
    void removeKafkaExtension() throws InterruptedException {
        try (
            final HiveMQContainer hivemq = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq4").withTag("4.7.4"))
                .withExtension(hiveMQExtension)
                .waitForExtension(hiveMQExtension)
                .withoutPrepackagedExtensions("hivemq-kafka-extension")
        ) {
            hivemq.start();

            final Mqtt5BlockingClient client = MqttClient
                .builder()
                .serverPort(hivemq.getMqttPort())
                .serverHost(hivemq.getHost())
                .useMqttVersion5()
                .buildBlocking();

            client.connect();
            final Mqtt5BlockingClient.Mqtt5Publishes publishes = client.publishes(MqttGlobalPublishFilter.ALL);
            client.subscribeWith().topicFilter("extensions").send();

            final Mqtt5Publish receive = publishes.receive();
            assertThat(receive.getPayload().isPresent()).isTrue();
            final String extensionInfo = new String(receive.getPayloadAsBytes());

            assertThat(extensionInfo).contains("hivemq-allow-all-extension");
            assertThat(extensionInfo).doesNotContain("hivemq-kafka-extension");
            assertThat(extensionInfo).contains("hivemq-bridge-extension");
            assertThat(extensionInfo).contains("hivemq-enterprise-security-extension");
        }
    }

    public static class CheckerExtension implements ExtensionMain {

        @Override
        public void extensionStart(
            final @NotNull ExtensionStartInput extensionStartInput,
            final @NotNull ExtensionStartOutput extensionStartOutput
        ) {
            final String extensionFolders = Arrays
                .stream(extensionStartInput.getServerInformation().getExtensionsFolder().listFiles())
                .filter(File::isDirectory)
                .map(File::getName)
                .collect(Collectors.joining("\n"));

            final byte[] bytes = extensionFolders.getBytes(StandardCharsets.UTF_8);
            Services
                .publishService()
                .publish(Builders.publish().topic("extensions").retain(true).payload(ByteBuffer.wrap(bytes)).build());

            Services
                .securityRegistry()
                .setAuthenticatorProvider(authenticatorProviderInput -> {
                    return (SimpleAuthenticator) (simpleAuthInput, simpleAuthOutput) -> {
                        simpleAuthOutput.authenticateSuccessfully();
                    };
                });
        }

        @Override
        public void extensionStop(
            final @NotNull ExtensionStopInput extensionStopInput,
            final @NotNull ExtensionStopOutput extensionStopOutput
        ) {}
    }
}
