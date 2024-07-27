package org.testcontainers.kafka;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.testcontainers.AbstractKafka;

@RunWith(Parameterized.class)
public class KafkaContainerTest extends AbstractKafka {

    @Parameterized.Parameters(name = "{0}")
    public static String[] params() {
        return new String[] { "apache/kafka:3.7.0", "apache/kafka-native:3.8.0-rc3" };
    }

    @Parameterized.Parameter
    public String imageName;

    @Test
    public void testUsage() throws Exception {
        try (KafkaContainer kafka = new KafkaContainer(imageName)) {
            kafka.start();
            testKafkaFunctionality(kafka.getBootstrapServers());
        }
    }
}
