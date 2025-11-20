package org.testcontainers.rabbitmq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import org.junit.jupiter.api.Test;
import org.testcontainers.utility.MountableFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMQContainerTest {

    public static final int DEFAULT_AMQPS_PORT = 5671;

    public static final int DEFAULT_AMQP_PORT = 5672;

    public static final int DEFAULT_HTTPS_PORT = 15671;

    public static final int DEFAULT_HTTP_PORT = 15672;

    @Test
    void shouldCreateRabbitMQContainer() {
        try (RabbitMQContainer container = new RabbitMQContainer(RabbitMQTestImages.RABBITMQ_IMAGE)) {
            container.start();

            assertThat(container.getAdminPassword()).isEqualTo("guest");
            assertThat(container.getAdminUsername()).isEqualTo("guest");

            assertThat(container.getAmqpsUrl())
                .isEqualTo(String.format("amqps://%s:%d", container.getHost(), container.getAmqpsPort()));
            assertThat(container.getAmqpUrl())
                .isEqualTo(String.format("amqp://%s:%d", container.getHost(), container.getAmqpPort()));
            assertThat(container.getHttpsUrl())
                .isEqualTo(String.format("https://%s:%d", container.getHost(), container.getHttpsPort()));
            assertThat(container.getHttpUrl())
                .isEqualTo(String.format("http://%s:%d", container.getHost(), container.getHttpPort()));

            assertThat(container.getLivenessCheckPortNumbers())
                .containsExactlyInAnyOrder(
                    container.getMappedPort(DEFAULT_AMQP_PORT),
                    container.getMappedPort(DEFAULT_AMQPS_PORT),
                    container.getMappedPort(DEFAULT_HTTP_PORT),
                    container.getMappedPort(DEFAULT_HTTPS_PORT)
                );

            assertFunctionality(container);
        }
    }

    @Test
    void shouldCreateRabbitMQContainerWithCustomCredentials() {
        try (
            RabbitMQContainer container = new RabbitMQContainer(RabbitMQTestImages.RABBITMQ_IMAGE)
                .withAdminUser("admin")
                .withAdminPassword("admin")
        ) {
            container.start();

            assertThat(container.getAdminPassword()).isEqualTo("admin");
            assertThat(container.getAdminUsername()).isEqualTo("admin");

            assertFunctionality(container);
        }
    }

    @Test
    void shouldMountConfigurationFile() {
        try (RabbitMQContainer container = new RabbitMQContainer(RabbitMQTestImages.RABBITMQ_IMAGE)) {
            container.withRabbitMQConfig(MountableFile.forClasspathResource("/rabbitmq-custom.conf"));
            container.start();

            assertThat(container.getLogs()).contains("debug"); // config file changes log level to `debug`
        }
    }

    @Test
    void shouldMountConfigurationFileErlang() {
        try (RabbitMQContainer container = new RabbitMQContainer(RabbitMQTestImages.RABBITMQ_IMAGE)) {
            container.withRabbitMQConfigErlang(MountableFile.forClasspathResource("/rabbitmq-custom.config"));
            container.start();

            assertThat(container.getLogs()).contains("debug"); // config file changes log level to `debug`
        }
    }

    @Test
    void shouldMountConfigurationFileSysctl() {
        try (RabbitMQContainer container = new RabbitMQContainer(RabbitMQTestImages.RABBITMQ_IMAGE)) {
            container.withRabbitMQConfigSysctl(MountableFile.forClasspathResource("/rabbitmq-custom.conf"));
            container.start();

            assertThat(container.getLogs()).contains("debug"); // config file changes log level to `debug`
        }
    }

    private void assertFunctionality(RabbitMQContainer container) {
        String queueName = "test-queue";
        String text = "Hello World!";

        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(container.getHost());
        factory.setPort(container.getAmqpPort());
        factory.setUsername(container.getAdminUsername());
        factory.setPassword(container.getAdminPassword());
        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
            channel.queueDeclare(queueName, false, false, false, null);
            channel.basicPublish("", queueName, null, text.getBytes());

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                assertThat(message).isEqualTo(text);
            };
            channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
        } catch (IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }
}
