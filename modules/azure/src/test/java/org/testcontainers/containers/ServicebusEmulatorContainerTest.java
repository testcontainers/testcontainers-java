package org.testcontainers.containers;

import com.azure.core.util.IterableStream;
import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusReceivedMessage;
import com.azure.messaging.servicebus.ServiceBusReceiverClient;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.messaging.servicebus.models.ServiceBusReceiveMode;
import org.junit.Rule;
import org.junit.Test;
import org.testcontainers.utility.DockerImageName;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class ServicebusEmulatorContainerTest {
    @Rule
    public ServicebusEmulatorContainer servicebusEmulatorContainer = new ServicebusEmulatorContainer(
        DockerImageName.parse("mcr.microsoft.com/azure-messaging/servicebus-emulator")
    );

    @Test
    public void testWithDefaultConfig() {
        List<String> sentMessages = Arrays.asList("Hello World");
        try(ServiceBusSenderClient sender = new ServiceBusClientBuilder()
            .connectionString(servicebusEmulatorContainer.getConnectionString())
            .sender()
            .queueName("queue.1")
            .buildClient()) {
            for (String m : sentMessages) {
                sender.sendMessage(new ServiceBusMessage(m));
            }
        }
        try(ServiceBusReceiverClient reciever = new ServiceBusClientBuilder()
            .connectionString(servicebusEmulatorContainer.getConnectionString())
            .receiver()
            .queueName("queue.1")
            .receiveMode(ServiceBusReceiveMode.RECEIVE_AND_DELETE)
            .buildClient()) {
            IterableStream<ServiceBusReceivedMessage> messagesStream = reciever.receiveMessages(sentMessages.size());
            List<String> recievedMessages = messagesStream.stream().map(m -> m.getBody().toString()).collect(Collectors.toList());
            assertThat(recievedMessages).isEqualTo(sentMessages);
        }
    }


    @Test
    public void testWithCustomConfig() {
        List<String> sentMessages = Arrays.asList("Hello World");
        try(ServiceBusSenderClient sender = new ServiceBusClientBuilder()
            .connectionString(servicebusEmulatorContainer.getConnectionString())
            .sender()
            .queueName("queue.666")
            .buildClient()) {
            for (String m : sentMessages) {
                sender.sendMessage(new ServiceBusMessage(m));
            }
        }
        try(ServiceBusReceiverClient reciever = new ServiceBusClientBuilder()
            .connectionString(servicebusEmulatorContainer.getConnectionString())
            .receiver()
            .queueName("queue.666")
            .receiveMode(ServiceBusReceiveMode.RECEIVE_AND_DELETE)
            .buildClient()) {
            IterableStream<ServiceBusReceivedMessage> messagesStream = reciever.receiveMessages(sentMessages.size());
            List<String> recievedMessages = messagesStream.stream().map(m -> m.getBody().toString()).collect(Collectors.toList());
            assertThat(recievedMessages).isEqualTo(sentMessages);
        }
    }
}
