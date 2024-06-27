package org.testcontainers.redpanda;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class CompatibleImageTest extends AbstractRedpanda {

    private final String image;

    public CompatibleImageTest(String image) {
        this.image = image;
    }

    @Parameterized.Parameters(name = "{0}")
    public static String[] image() {
        return new String[] { "docker.redpanda.com/vectorized/redpanda:v22.2.1", "redpandadata/redpanda:v22.2.1" };
    }

    @Test
    public void shouldProduceAndConsumeMessage() throws Exception {
        try (RedpandaContainer container = new RedpandaContainer(this.image)) {
            container.start();
            testKafkaFunctionality(container.getBootstrapServers());
        }
    }
}
