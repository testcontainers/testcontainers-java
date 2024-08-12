package org.testcontainers.kafka;

import org.junit.Test;
import org.testcontainers.AbstractKafka;

public class ConfluentKafkaContainerTest extends AbstractKafka {

    @Test
    public void testUsage() throws Exception {
        try (ConfluentKafkaContainer kafka = new ConfluentKafkaContainer("confluentinc/cp-kafka:7.4.0")) {
            kafka.start();
            testKafkaFunctionality(kafka.getBootstrapServers());
        }
    }
}
