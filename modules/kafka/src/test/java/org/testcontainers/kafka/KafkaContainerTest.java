package org.testcontainers.kafka;

import org.junit.Test;
import org.testcontainers.AbstractKafka;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.SocatContainer;

public class KafkaContainerTest extends AbstractKafka {

    @Test
    public void testUsage() throws Exception {
        try ( // constructorWithVersion {
            KafkaContainer kafka = new KafkaContainer("apache/kafka-native:3.8.0")
            // }
        ) {
            kafka.start();
            testKafkaFunctionality(kafka.getBootstrapServers());
        }
    }

    @Test
    public void testUsageWithNetworkAlias() {
        try (
            // registerAlias {
            Network network = Network.newNetwork();
            KafkaContainer kafka = new KafkaContainer("apache/kafka-native:3.8.0")
                .withNetworkAliases("kafka")
                .withNetwork(network);
            // }
        ) {
            kafka.start();
            assertKafka("kafka:9092", network);
        }
    }

    @Test
    public void testUsageWithListener() {
        try (
            Network network = Network.newNetwork();
            // registerListener {
            KafkaContainer kafka = new KafkaContainer("apache/kafka-native:3.8.0")
                .withListener("kafka:19092")
                .withNetwork(network);
            // }
        ) {
            kafka.start();
            assertKafka("kafka:19092", network);
        }
    }

    @Test
    public void testUsageWithListenerFromProxy() throws Exception {
        try (
            Network network = Network.newNetwork();
            SocatContainer socat = new SocatContainer().withNetwork(network).withTarget(2000, "kafka", 19092);
            KafkaContainer kafka = new KafkaContainer("apache/kafka-native:3.8.0")
                .withListener("kafka:19092", () -> socat.getHost() + ":" + socat.getMappedPort(2000))
                .withNetwork(network)
        ) {
            socat.start();
            kafka.start();

            String bootstrapServers = String.format("%s:%s", socat.getHost(), socat.getMappedPort(2000));
            testKafkaFunctionality(bootstrapServers);
        }
    }
}
