package org.testcontainers.containers;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.ClassRule;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Test for basic functionality when used as a <code>@ClassRule</code>.
 *
 * @author Michael J. Simons
 */
public class RabbitMQContainerJUnitIntegrationTest {

    @ClassRule
    public static RabbitMQContainer rabbitMQContainer = new RabbitMQContainer();

    @Test
    public void shouldStart() {
        boolean actual = rabbitMQContainer.isRunning();
        assertThat(actual).isTrue();
    }

    @Test
    public void shouldConnectOverAmqp() {
        assertThatCode(() -> {
            ConnectionFactory connectionFactory = new ConnectionFactory();
            connectionFactory.setUri(rabbitMQContainer.getAmqpUrl());
            connectionFactory.setPassword(rabbitMQContainer.getAdminPassword());
            Connection connection = connectionFactory.newConnection();
            Channel channel = connection.openChannel().orElseThrow(() -> new RuntimeException("Failed to Open channel"));
            channel.close();
            connection.close();
        }).doesNotThrowAnyException();
    }

}
