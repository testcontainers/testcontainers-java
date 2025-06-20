package org.testcontainers.kafka;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.Parameter;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;
import org.testcontainers.AbstractKafka;

@ParameterizedClass
@MethodSource("params")
public class CompatibleApacheKafkaImageTest extends AbstractKafka {

    public static String[] params() {
        return new String[] { "apache/kafka:3.8.0", "apache/kafka-native:3.8.0" };
    }

    @Parameter(0)
    public String imageName;

    @Test
    public void testUsage() throws Exception {
        try (KafkaContainer kafka = new KafkaContainer(this.imageName)) {
            kafka.start();
            testKafkaFunctionality(kafka.getBootstrapServers());
        }
    }
}
