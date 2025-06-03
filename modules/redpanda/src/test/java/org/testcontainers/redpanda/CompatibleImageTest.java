package org.testcontainers.redpanda;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedClass;
import org.junit.jupiter.params.provider.MethodSource;

@ParameterizedClass
@MethodSource("image")
public class CompatibleImageTest extends AbstractRedpanda {

    private final String image;

    public CompatibleImageTest(String image) {
        this.image = image;
    }

    public static String[] image() {
        return new String[] { "docker.redpanda.com/redpandadata/redpanda:v22.2.1", "redpandadata/redpanda:v22.2.1" };
    }

    @Test
    public void shouldProduceAndConsumeMessage() throws Exception {
        try (RedpandaContainer container = new RedpandaContainer(this.image)) {
            container.start();
            testKafkaFunctionality(container.getBootstrapServers());
        }
    }
}
