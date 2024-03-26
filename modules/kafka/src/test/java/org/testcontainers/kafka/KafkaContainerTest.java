package org.testcontainers.kafka;

import org.junit.Test;
import org.testcontainers.AbstractKafka;

public class KafkaContainerTest extends AbstractKafka {

    @Test
    public void testUsage() throws Exception {
        try (KafkaContainer kafka = new KafkaContainer("apache/kafka:3.7.0")) {
            kafka.start();
            testKafkaFunctionality(kafka.getBootstrapServers());
        }
    }
}
