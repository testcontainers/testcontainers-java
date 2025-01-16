package org.testcontainers.azure;

import com.azure.core.util.IterableStream;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class AzureServicebusEmulatorContainerTest {

    @Test
    public void testWithDefaultConfig() {
        try(
            // emulatorContainerDefaultConfig {
            AzureServicebusEmulatorContainer azureServicebusEmulatorContainer = new AzureServicebusEmulatorContainer(
                DockerImageName.parse("mcr.microsoft.com/azure-messaging/servicebus-emulator")
            )
            // }
        ) {
            sendAndReceive(azureServicebusEmulatorContainer, "queue.1");
        }
    }

    @Test
    public void testWithCustomConfig() {
        try(
        // emulatorContainerCustomConfig {
        AzureServicebusEmulatorContainer azureServicebusEmulatorContainer = new AzureServicebusEmulatorContainer(
            DockerImageName.parse("mcr.microsoft.com/azure-messaging/servicebus-emulator")
        ).withConfigFile(MountableFile.forClasspathResource("/servicebus-config.json"))
        // }
        ) {
            sendAndReceive(azureServicebusEmulatorContainer, "our.queue");
        }
    }

    private static void sendAndReceive(AzureServicebusEmulatorContainer azureServicebusEmulatorContainer, String queueName) {
        List<String> sentMessages = Arrays.asList("Hello World");
        try (
            // buildClient {
            ServiceBusSenderClient sender = new ServiceBusClientBuilder()
            .connectionString(azureServicebusEmulatorContainer.getConnectionString())
            .sender()
            .queueName(queueName)
            .buildClient()
            // }
        ) {
            for (String m : sentMessages) {
                sender.sendMessage(new ServiceBusMessage(m));
            }
        }
        try (ServiceBusReceiverClient reciever = new ServiceBusClientBuilder()
            .connectionString(azureServicebusEmulatorContainer.getConnectionString())
            .receiver()
            .queueName(queueName)
            .receiveMode(ServiceBusReceiveMode.RECEIVE_AND_DELETE)
            .buildClient()) {
            IterableStream<ServiceBusReceivedMessage> messagesStream = reciever.receiveMessages(sentMessages.size());
            List<String> recievedMessages = messagesStream.stream().map(m -> m.getBody().toString()).collect(Collectors.toList());
            assertThat(recievedMessages).isEqualTo(sentMessages);
        }
    }
}
