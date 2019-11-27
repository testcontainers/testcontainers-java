package rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.RabbitMQContainer;

public class RabbitMQPluginsTest {

    // plugins {
    @ClassRule
    public static RabbitMQContainer rabbitmq = new RabbitMQContainer()
        .withPluginsEnabled("rabbitmq_shovel", "rabbitmq_random_exchange");
    // }

    @Test
    public void test() throws IOException, InterruptedException {
        assertTrue(rabbitmq.isRunning()); // good enough to check that the container started listening

        assertThat(rabbitmq.execInContainer("rabbitmq-plugins", "is_enabled", "rabbitmq_shovel", "--quiet")
            .getStdout())
            .contains("rabbitmq_shovel is enabled");

        assertThat(rabbitmq.execInContainer("rabbitmq-plugins", "is_enabled", "rabbitmq_random_exchange", "--quiet")
            .getStdout())
            .contains("rabbitmq_random_exchange is enabled");
    }

}
