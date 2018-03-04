package org.testcontainers.containers;

import org.junit.Test;

import java.util.stream.Stream;

public class NewDSLTest {

    @Test
    public void justTest() throws Exception {
        try (
            Network network = Network.newNetwork();

            KafkaContainer kafka = new KafkaContainer(b -> {
                // from GenericContainer.Builder
                b.withNetwork(network);

                // from KafkaContainer.Builder
                b.withExternalZookeeper("zookeeper:2181");
            });

            GenericContainer zookeeper = new GenericContainer<>("confluentinc/cp-zookeeper:4.0.0", b -> {
                // Why not?
                network.applyTo(b);

                b.withNetworkAliases("zookeeper");
                b.withEnv("ZOOKEEPER_CLIENT_PORT", "2181");
            });
        ) {
            Stream.of(kafka, zookeeper).parallel().forEach(GenericContainer::start);

            KafkaContainerTest.testKafkaFunctionality(kafka.getBootstrapServers());
        }
    }
}
