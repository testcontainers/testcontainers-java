package org.testcontainers.redpanda;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class CompatibleImageTest extends AbstractRedpanda {

    public static String[] image() {
        return new String[] { "docker.redpanda.com/redpandadata/redpanda:v22.2.1", "redpandadata/redpanda:v22.2.1" };
    }

    @ParameterizedTest
    @MethodSource("image")
    void shouldProduceAndConsumeMessage(String image) throws Exception {
        try (RedpandaContainer container = new RedpandaContainer(image)) {
            container.start();
            testKafkaFunctionality(container.getBootstrapServers());
        }
    }
}
