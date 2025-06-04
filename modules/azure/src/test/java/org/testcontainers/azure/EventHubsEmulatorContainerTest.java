package org.testcontainers.azure;

import com.azure.core.util.IterableStream;
import com.azure.messaging.eventhubs.EventData;
import com.azure.messaging.eventhubs.EventHubClientBuilder;
import com.azure.messaging.eventhubs.EventHubConsumerClient;
import com.azure.messaging.eventhubs.EventHubProducerClient;
import com.azure.messaging.eventhubs.models.EventPosition;
import com.azure.messaging.eventhubs.models.PartitionEvent;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.ManagedNetwork;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.MountableFile;

import java.time.Duration;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.waitAtMost;

@Testcontainers
public class EventHubsEmulatorContainerTest {

    // network {
    @ManagedNetwork
    public Network network = Network.newNetwork();

    // }

    // azuriteContainer {
    @Container
    public AzuriteContainer azuriteContainer = new AzuriteContainer("mcr.microsoft.com/azure-storage/azurite:3.33.0")
        .withNetwork(network);

    // }

    // emulatorContainer {
    @Container
    public EventHubsEmulatorContainer emulator = new EventHubsEmulatorContainer(
        "mcr.microsoft.com/azure-messaging/eventhubs-emulator:2.0.1"
    )
        .acceptLicense()
        .withNetwork(network)
        .withConfig(MountableFile.forClasspathResource("/eventhubs_config.json"))
        .withAzuriteContainer(azuriteContainer);

    // }

    @Test
    public void testWithEventHubsClient() {
        try (
            // createProducerAndConsumer {
            EventHubProducerClient producer = new EventHubClientBuilder()
                .connectionString(emulator.getConnectionString())
                .fullyQualifiedNamespace("emulatorNs1")
                .eventHubName("eh1")
                .buildProducerClient();
            EventHubConsumerClient consumer = new EventHubClientBuilder()
                .connectionString(emulator.getConnectionString())
                .fullyQualifiedNamespace("emulatorNs1")
                .eventHubName("eh1")
                .consumerGroup("cg1")
                .buildConsumerClient()
            // }
        ) {
            producer.send(Collections.singletonList(new EventData("test")));

            waitAtMost(Duration.ofSeconds(30))
                .pollDelay(Duration.ofSeconds(5))
                .untilAsserted(() -> {
                    IterableStream<PartitionEvent> events = consumer.receiveFromPartition(
                        "0",
                        1,
                        EventPosition.earliest(),
                        Duration.ofSeconds(2)
                    );
                    Optional<PartitionEvent> event = events.stream().findFirst();
                    assertThat(event).isPresent();
                    assertThat(event.get().getData().getBodyAsString()).isEqualTo("test");
                });
        }
    }
}
