package org.testcontainers.kafka;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.AbstractKafka;

public class CompatibleApacheKafkaImageTest extends AbstractKafka {

    public static String[] params() {
        return new String[] { "apache/kafka:3.8.0", "apache/kafka-native:3.8.0" };
    }

    @ParameterizedTest
    @MethodSource("params")
    public void testUsage(String imageName) throws Exception {
        try (KafkaContainer kafka = new KafkaContainer(imageName)) {
            kafka.start();
            testKafkaFunctionality(kafka.getBootstrapServers());
        }
    }
}
