package rabbitmq;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.TimeoutException;

import org.junit.ClassRule;
import org.junit.Test;
import org.testcontainers.containers.RabbitMQContainer;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class RabbitMQSimpleTest {

    // simple {
    @ClassRule
    public static RabbitMQContainer rabbitmq = new RabbitMQContainer();
    // }

    @Test
    public void test() throws NoSuchAlgorithmException, KeyManagementException, URISyntaxException, IOException, TimeoutException {
        assertTrue(rabbitmq.isRunning()); // good enough to check that the container started listening

        // connection {
        ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setUri(rabbitmq.getAmqpUrl());
        connectionFactory.setPassword(rabbitmq.getAdminPassword());
        try (Connection connection = connectionFactory.newConnection();
             Channel channel = connection.openChannel().orElseThrow(() -> new RuntimeException("Failed to Open channel"))) {
            assertThat(channel.isOpen()).isTrue();
        }
        // }
    }

}
